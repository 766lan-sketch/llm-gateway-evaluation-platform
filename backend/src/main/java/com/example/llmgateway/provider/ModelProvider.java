package com.example.llmgateway.provider;

/**
 * 所有模型共同遵守的统一接口，可以把它理解成“统一插座”。
 */
public interface ModelProvider {
    String id();
    String name();
    boolean available();
    String chat(String question);

// id()：模型的唯一编号。
// name()：页面显示的模型名称。
// available()：模型目前能否使用。
// chat()：接收问题并返回答案。
}
