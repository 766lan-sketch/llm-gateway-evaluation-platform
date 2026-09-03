package com.example.llmgateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 一条 CallLog 对象对应数据库中的一条模型调用记录。
 * Entity（实体类）可以先简单理解为“Java 版本的数据表”。
 */
@Entity
@Table(name = "call_logs")
public class CallLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(nullable = false, length = 2000)
    private String question;

    @Column(length = 20000)
    private String answer;

    @Column(nullable = false)
    private long durationMs;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 2000)
    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt;

    // JPA 创建实体对象时需要无参数构造方法。
    protected CallLog() {}

    private CallLog(String model, String question, String answer, long durationMs,
                    String status, String errorMessage, Instant createdAt) {
        this.model = model;
        this.question = question;
        this.answer = answer;
        this.durationMs = durationMs;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public static CallLog success(String model, String question, String answer, long durationMs) {
        return new CallLog(model, question, answer, durationMs, "SUCCESS", null, Instant.now());
    }

    public static CallLog failure(String model, String question, long durationMs, String errorMessage) {
        return new CallLog(model, question, null, durationMs, "FAILED", errorMessage, Instant.now());
    }

    public Long getId() { return id; }
    public String getModel() { return model; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public long getDurationMs() { return durationMs; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
