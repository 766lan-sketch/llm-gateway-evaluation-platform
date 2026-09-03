package com.example.llmgateway.dto;

import java.time.Instant;

/**
 * fallbackUsed 表示是否启用了备用模型；notice 用来告诉前端发生了什么。
 */
public record ChatResult(
        String answer,
        String model,
        long durationMs,
        Instant createdAt,
        boolean fallbackUsed,
        String notice) {}
