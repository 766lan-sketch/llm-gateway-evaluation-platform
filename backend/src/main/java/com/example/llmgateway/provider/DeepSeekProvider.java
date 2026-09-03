package com.example.llmgateway.provider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
//你会发现 DeepSeek也遵守完全相同的接口，但它的 chat() 内部会发送真实 HTTP 请求
public class DeepSeekProvider implements ModelProvider {
    // ObjectMapper 负责把 Java 对象转换成 JSON，也负责读取模型返回的 JSON。
    private final ObjectMapper objectMapper;
    // HttpClient 负责向 DeepSeek 服务器发送 HTTP 请求。
    private final HttpClient httpClient;
    private final String apiKey;
    private final String apiUrl;
    private final String model;
//apiKey：调用模型的密码。
//apiUrl：请求发送到哪个地址。
//model：使用哪个模型。
//API Key仍然从 Windows环境变量读取，没有写死在代码中。
    public DeepSeekProvider(ObjectMapper objectMapper,
            @Value("${ai.deepseek.api-key:}") String apiKey,
            @Value("${ai.deepseek.api-url}") String apiUrl,
            @Value("${ai.deepseek.model}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        // 最多等待 10 秒建立连接，避免网络故障时一直卡住。
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override public String id() { return "deepseek"; }
    @Override public String name() { return "DeepSeek"; }
    @Override public boolean available() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public String chat(String question) {
        // 没有 API Key 就立即提示，不发送无效请求。
        if (!available()) throw new IllegalStateException("DeepSeek API Key 尚未配置");
        try {
            // 按照大模型接口要求，组装将要发送的 JSON 请求体。
            Map<String, Object> body = Map.of(
                    "model", model,
                    "stream", false,
                    "messages", List.of(
                            Map.of("role", "system", "content", "你是一名简洁、可靠的中文助手。"),
                            Map.of("role", "user", "content", question)));
            // 创建 POST 请求，并把 API Key 放在 Authorization 请求头中。
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            // 发送请求，得到模型服务器返回的 JSON 字符串。
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("DeepSeek 请求失败，状态码：" + response.statusCode());
            }
            // 从 JSON 的 choices[0].message.content 中取出最终回答。
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("模型调用被中断", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("模型调用失败：" + exception.getMessage(), exception);
        }
    }
}
