package com.ooru.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class AssistantDtos {

    public static class AssistantState {
        public String category; // may be null until the assistant has matched one
        public Map<String, String> data = Map.of();
    }

    public static class ChatRequest {
        @NotNull
        public AssistantState state;
        @NotBlank
        public String message;
    }

    public static class ChatResponse {
        public String category;
        public Map<String, String> data;
        public boolean ready;
        public String reply;
    }

    public static class ConfirmRequest {
        @NotBlank
        public String category;
        @NotNull
        public Map<String, String> data;
    }
}
