package com.ip.camp.greetingsapp.dto;

public class Message {
    private String content;
    private String role;

    public Message() {
        //Empty constructor
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
