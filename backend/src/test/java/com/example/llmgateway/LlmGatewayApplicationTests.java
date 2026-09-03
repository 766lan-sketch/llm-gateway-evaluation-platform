package com.example.llmgateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.llmgateway.service.ModelRouterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LlmGatewayApplicationTests {
    @Autowired ModelRouterService service;

    @Test
    void localDemoModelWorksThroughUnifiedRouter() {
        var result = service.chat("local-demo", "测试统一接口");
        assertThat(result.answer()).contains("统一网关");
        assertThat(result.model()).isEqualTo("本地演示模型");
    }
}
