package com.ip.camp.greetingsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class LLMRequest {
    private String model;
    private List<Message> messages;
    @JsonProperty("max_tokens")
    private int maxTokens;

    public LLMRequest() {
        //Empty constructor
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
}
