package com.example.llmgateway.dto;

import com.example.llmgateway.entity.CallLog;
import java.time.Instant;

/** 返回给前端的调用记录，避免把数据库实体直接暴露给页面。 */
public record CallLogView(
        Long id,
        String model,
        String question,
        String answer,
        long durationMs,
        String status,
        String errorMessage,
        Instant createdAt) {

    public static CallLogView from(CallLog log) {
        return new CallLogView(log.getId(), log.getModel(), log.getQuestion(), log.getAnswer(),
                log.getDurationMs(), log.getStatus(), log.getErrorMessage(), log.getCreatedAt());
    }
}
