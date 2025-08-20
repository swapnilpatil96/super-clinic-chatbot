package com.springboot.chatgpt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        String role,                    // "user", "system", "assistant"
        String content,                 // Message content
        FunctionCall function_call      // Only present in assistant replies with function call
) {

    public ChatMessage {
        if (content == null) {
            content = "";
        }
    }


    public record FunctionCall(String name, String arguments) {
    }
}

