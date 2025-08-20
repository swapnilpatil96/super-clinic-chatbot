package com.springboot.chatgpt.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        List<FunctionDefinition> functions,
        FunctionCallRequest function_call,  // {"name": "recommend_doctor"} OR "auto"
        double temperature,
        int max_tokens
) {
    public record FunctionCallRequest(String name) {}
}

