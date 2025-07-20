package com.example.smartstudyassistant.api;

import java.util.List;

public class OpenAIResponse {
    private List<Choice> choices;

    public String getAnswer() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).message.content;
        }
        return null;
    }

    public static class Choice {
        private Message message;
    }

    public static class Message {
        private String content;
    }
}