package com.example.llmgateway.dto;

import java.util.List;

/** 整个平台的评测概览，以及每个模型各自的统计结果。 */
public record EvaluationStats(
        long totalCalls,
        long successfulCalls,
        double successRate,
        double averageDurationMs,
        List<ModelEvaluation> models) {}
