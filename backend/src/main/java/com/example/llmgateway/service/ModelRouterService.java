package com.example.llmgateway.service;

import com.example.llmgateway.dto.ChatResult;
import com.example.llmgateway.dto.CallLogView;
import com.example.llmgateway.dto.EvaluationStats;
import com.example.llmgateway.dto.ModelEvaluation;
import com.example.llmgateway.dto.ModelInfo;
import com.example.llmgateway.entity.CallLog;
import com.example.llmgateway.provider.ModelProvider;
import com.example.llmgateway.repository.CallLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ModelRouterService {
    private static final String FALLBACK_MODEL_ID = "local-demo";
    // key 是模型编号，value 是对应的模型对象，例如 deepseek -> DeepSeekProvider。
    private final Map<String, ModelProvider> providers;
    private final CallLogRepository callLogRepository;

    public ModelRouterService(List<ModelProvider> providers, CallLogRepository callLogRepository) {
        // Spring 会自动找到所有 ModelProvider 实现，再把它们从 List 转成方便查询的 Map。
        this.providers = providers.stream().collect(Collectors.toMap(ModelProvider::id, Function.identity()));
        this.callLogRepository = callLogRepository;
    }

    public List<ModelInfo> models() {
        // 将所有模型的编号、名称和可用状态返回给前端。
        return providers.values().stream()
                .map(provider -> new ModelInfo(provider.id(), provider.name(), provider.available()))
                .sorted((left, right) -> left.id().compareTo(right.id()))
                .toList();
    }

    public ChatResult chat(String modelId, String question) {
        String cleanQuestion = question.trim();
        ModelProvider provider = providers.get(modelId);

        // 模型编号写错属于请求错误，不应该悄悄切换模型。
        if (provider == null) {
            callLogRepository.save(CallLog.failure(modelId, cleanQuestion, 0, "不支持的模型"));
            throw new IllegalArgumentException("不支持的模型：" + modelId);
        }

        long requestStartedAt = System.nanoTime();
        long primaryStartedAt = System.nanoTime();

        try {
            // 第一次先调用用户选择的主模型。
            String answer = provider.chat(cleanQuestion);
            long durationMs = elapsedMs(primaryStartedAt);

            callLogRepository.save(CallLog.success(provider.name(), cleanQuestion, answer, durationMs));
            return new ChatResult(answer, provider.name(), durationMs, Instant.now(), false, null);
        } catch (RuntimeException primaryException) {
            // 主模型失败也保存下来，这样评测页面能够统计失败率。
            callLogRepository.save(CallLog.failure(
                    provider.name(), cleanQuestion, elapsedMs(primaryStartedAt), primaryException.getMessage()));

            ModelProvider fallbackProvider = providers.get(FALLBACK_MODEL_ID);
            // 本地演示模型自身失败时不能再次选择自己，否则会形成无限循环。
            if (fallbackProvider == null || fallbackProvider == provider || !fallbackProvider.available()) {
                throw primaryException;
            }

            return callFallback(fallbackProvider, provider, cleanQuestion, requestStartedAt, primaryException);
        }
    }

    private ChatResult callFallback(ModelProvider fallbackProvider, ModelProvider primaryProvider,
                                    String question, long requestStartedAt, RuntimeException primaryException) {
        long fallbackStartedAt = System.nanoTime();
        try {
            // 主模型报错后，使用相同的问题调用备用模型。
            String answer = fallbackProvider.chat(question);
            long fallbackDurationMs = elapsedMs(fallbackStartedAt);
            callLogRepository.save(CallLog.success(
                    fallbackProvider.name(), question, answer, fallbackDurationMs));

            String notice = "主模型“" + primaryProvider.name() + "”调用失败，已自动切换到备用模型。";
            // 返回给用户的是整个请求耗时，数据库则分别记录两个模型各自的耗时。
            return new ChatResult(answer, fallbackProvider.name(), elapsedMs(requestStartedAt),
                    Instant.now(), true, notice);
        } catch (RuntimeException fallbackException) {
            callLogRepository.save(CallLog.failure(
                    fallbackProvider.name(), question, elapsedMs(fallbackStartedAt), fallbackException.getMessage()));
            throw new IllegalStateException(
                    "主模型和备用模型均不可用：" + primaryException.getMessage(), fallbackException);
        }
    }

    public List<CallLogView> recentCalls() {
        // 实体转成 DTO 后再返回，保持数据库层和接口层分离。
        return callLogRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(CallLogView::from)
                .toList();
    }

    public EvaluationStats evaluationStats() {
        // 评测数据来自数据库中的真实调用记录，而不是前端临时拼出来的数字。
        List<CallLog> calls = callLogRepository.findAll();
        long totalCalls = calls.size();
        long successfulCalls = calls.stream().filter(this::isSuccessful).count();

        // groupingBy 会把相同模型的记录放进同一个小组，方便分别计算。
        Map<String, List<CallLog>> callsByModel = calls.stream()
                .collect(Collectors.groupingBy(CallLog::getModel));

        List<ModelEvaluation> modelEvaluations = callsByModel.entrySet().stream()
                .map(entry -> evaluateModel(entry.getKey(), entry.getValue()))
                .sorted((left, right) -> left.model().compareTo(right.model()))
                .toList();

        return new EvaluationStats(
                totalCalls,
                successfulCalls,
                percentage(successfulCalls, totalCalls),
                averageDuration(calls),
                modelEvaluations);
    }

    private ModelEvaluation evaluateModel(String model, List<CallLog> calls) {
        long successes = calls.stream().filter(this::isSuccessful).count();
        return new ModelEvaluation(
                model,
                calls.size(),
                successes,
                percentage(successes, calls.size()),
                averageDuration(calls));
    }

    private boolean isSuccessful(CallLog log) {
        return "SUCCESS".equals(log.getStatus());
    }

    private double percentage(long part, long total) {
        return total == 0 ? 0 : roundOne(part * 100.0 / total);
    }

    private double averageDuration(List<CallLog> calls) {
        double average = calls.stream().mapToLong(CallLog::getDurationMs).average().orElse(0);
        return roundOne(average);
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
