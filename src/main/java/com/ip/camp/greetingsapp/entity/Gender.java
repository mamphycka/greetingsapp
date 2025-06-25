package com.ip.camp.greetingsapp.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Gender {
    FEMALE("female"),
    MALE("male");

    private final String value;

    Gender(String gender) {
        this.value = gender;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
