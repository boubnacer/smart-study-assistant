package com.example.smartstudyassistant.api;

import java.util.List;

public class OpenAIRequest {
    private String model;
    private List<Message> messages;
    private double temperature;

    public OpenAIRequest(String model, List<Message> messages, double temperature) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
    }

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}