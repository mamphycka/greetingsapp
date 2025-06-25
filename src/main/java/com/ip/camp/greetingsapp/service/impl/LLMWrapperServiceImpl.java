package com.ip.camp.greetingsapp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ip.camp.greetingsapp.dto.LLMRequest;
import com.ip.camp.greetingsapp.dto.Message;
import com.ip.camp.greetingsapp.exception.LLMServiceException;
import com.ip.camp.greetingsapp.service.LLMWrapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;

@Service
public class LLMWrapperServiceImpl implements LLMWrapperService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LLMWrapperServiceImpl.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    @Value("${llm.request.url}")
    private String url;
    @Value("${llm.model.name}")
    private String modelName;
    @Value("${llm.model.max.tokens}")
    private int maxTokens;
    @Value("${llm.model.message.role}")
    private String user;

    public LLMWrapperServiceImpl(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * Function to connect to a GPT4ALL LLM using API call.
     */
    @Override
    public String generateGreeting(String name, int age) {
        try {
            LLMRequest llmRequest = new LLMRequest();
            llmRequest.setModel(this.modelName);
            llmRequest.setMaxTokens(this.maxTokens);

            Message message = new Message();
            message.setRole(this.user);

            String promptContent = String.format(
                    "Generate a birthday greeting for: \"%s\", who is: \"%d\" old. Only return the greeting message.",
                    name, age
            );
            message.setContent(promptContent);

            llmRequest.setMessages(Collections.singletonList(message));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(this.url))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(llmRequest)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response == null || response.body() == null || response.body().isEmpty()) {
                throw new LLMServiceException("LLM API returned an empty or null response body.");
            }

            if (response.statusCode() != 200) {
                LOGGER.error("LLM API returned non-200 status code: {}. Response body: {}",
                        response.statusCode(), response.body());
                throw new LLMServiceException(
                        String.format("LLM API call failed with status %d: %s", response.statusCode(), response.body())
                );
            }

            JsonNode jsonNode = objectMapper.readTree(response.body());
            JsonNode choices = jsonNode.get("choices");

            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                LOGGER.warn("LLM response did not contain expected 'choices' array or it was empty. Response: {}", response.body());
                throw new LLMServiceException("LLM response structure is unexpected: 'choices' array not found or empty.");
            }

            String greetings = null;
            for (JsonNode choice : choices) {
                JsonNode outputMessage = choice.get("message");
                if (outputMessage != null) {
                    JsonNode content = outputMessage.get("content");
                    if (content != null) {
                        greetings = content.asText();
                        LOGGER.info("Successfully generated greeting for {}: {}", name, greetings);
                        break;
                    }
                }
            }
            return greetings;
        } catch (URISyntaxException e) {
            LOGGER.error("Invalid LLM API URL configured: {}", url, e);
            throw new LLMServiceException("Invalid LLM API URL configuration.");
        } catch (JsonProcessingException e) {
            LOGGER.error("Error processing JSON for LLM request or response: {}", e.getMessage(), e);
            throw new LLMServiceException("Error processing JSON with LLM service.");
        } catch (IOException e) {
            LOGGER.error("Network or I/O error during LLM API call: {}", e.getMessage(), e);
            throw new LLMServiceException("Network or I/O error when communicating with LLM service.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("LLM API call was interrupted: {}", e.getMessage(), e);
            throw new LLMServiceException("LLM API call was interrupted.");
        } catch (LLMServiceException e) {
            // Re-throw our custom exception
            throw e;
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred during generateGreeting(): {}", e.getMessage(), e);
            throw new LLMServiceException("An unexpected error occurred while generating greeting.");
        }
    }
}
