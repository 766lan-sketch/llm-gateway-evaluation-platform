package com.example.llmgateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "请选择模型") String model,
        @NotBlank(message = "问题不能为空") @Size(max = 1000) String question) {}
