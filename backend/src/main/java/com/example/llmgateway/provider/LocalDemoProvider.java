package com.example.llmgateway.provider;

import org.springframework.stereotype.Component;

@Component
//本地演示模型承诺遵守 ModelProvider 这套统一规则，因此它实现了 ModelProvider 接口。
public class LocalDemoProvider implements ModelProvider {
    @Override public String id() { return "local-demo"; }
    @Override public String name() { return "本地演示模型"; }
    @Override public boolean available() { return true; }

    @Override
    public String chat(String question) {
        // 不访问互联网，方便我们先测试统一模型接口是否正确。
        return "这是本地演示模型的回答。统一网关已经收到你的问题：“" + question + "”。";
    }
}
