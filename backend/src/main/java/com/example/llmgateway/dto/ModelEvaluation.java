package com.example.llmgateway.dto;

/** 单个模型的统计结果。 */
public record ModelEvaluation(
        String model,
        long totalCalls,
        long successfulCalls,
        double successRate,
        double averageDurationMs) {}
