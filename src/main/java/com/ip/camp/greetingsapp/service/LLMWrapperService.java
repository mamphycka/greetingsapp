package com.ip.camp.greetingsapp.service;

/**
 * Interface to provide definition for local LLMs.
 */
public interface LLMWrapperService {

    /**
     * Generating personalized greeting for a person based on it's name and age.
     *
     * @param name - People's name
     * @param age  - People's age
     * @return personalized greeting message
     */
    String generateGreeting(String name, int age);
}
