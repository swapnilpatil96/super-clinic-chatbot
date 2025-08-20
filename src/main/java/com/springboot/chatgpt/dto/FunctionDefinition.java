package com.springboot.chatgpt.dto;

import java.util.List;
import java.util.Map;

public record FunctionDefinition(
        String name,
        String description,
        Parameters parameters
) {
    public record Parameters(
            String type,
            Map<String, Property> properties,
            List<String> required
    ) {
        public Parameters(Map<String, Property> properties, List<String> required) {
            this("object", properties, required); // Always type = object
        }

        public record Property(
                String type,
                String format // Optional: "date", etc.
        ) {
        }
    }
}

