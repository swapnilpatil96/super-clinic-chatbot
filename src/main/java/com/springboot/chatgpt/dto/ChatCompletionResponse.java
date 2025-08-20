package com.springboot.chatgpt.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatCompletionResponse(
        List<Choice> choices
) {
    public record Choice(ChatMessage message) {}
}

