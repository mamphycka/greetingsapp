package com.ip.camp.greetingsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ip.camp.greetingsapp.dto.LLMRequest;
import com.ip.camp.greetingsapp.exception.LLMServiceException;
import com.ip.camp.greetingsapp.service.impl.LLMWrapperServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LLMWrapperServiceImplTest {

    // Test data
    private static final String TEST_NAME = "Alice";
    private static final int TEST_AGE = 30;
    private static final String EXPECTED_GREETING = "Happy Birthday, Alice! Enjoy your 30th!";
    private static final String LLM_API_URL = "http://localhost:8080/llm-api";
    private static final String LLM_MODEL_NAME = "test-model";
    private static final int LLM_MAX_TOKENS = 100;
    private static final String LLM_MESSAGE_ROLE = "user";
    // Mock dependencies
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<String> httpResponse;
    // Inject mocks into the service
    @InjectMocks
    private LLMWrapperServiceImpl llmWrapperService;

    private void setUp() {
        this.setUp(false);
    }

    private void setUp(boolean withoutResponse) {
        // Use ReflectionTestUtils to set @Value fields for the unit test instance
        ReflectionTestUtils.setField(llmWrapperService, "url", LLM_API_URL);
        ReflectionTestUtils.setField(llmWrapperService, "modelName", LLM_MODEL_NAME);
        ReflectionTestUtils.setField(llmWrapperService, "maxTokens", LLM_MAX_TOKENS);
        ReflectionTestUtils.setField(llmWrapperService, "user", LLM_MESSAGE_ROLE);

        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"model\":\"test-model\",\"maxTokens\":100,\"messages\":[{\"role\":\"user\",\"content\":\"Generate a birthday greeting for: Alice, who is: 30 years old. Only return the greeting message.\"}]}");

            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            ArrayNode choicesArray = JsonNodeFactory.instance.arrayNode();
            ObjectNode choiceNode = JsonNodeFactory.instance.objectNode();
            ObjectNode messageNode = JsonNodeFactory.instance.objectNode();

            messageNode.put("content", EXPECTED_GREETING);
            choiceNode.set("message", messageNode);
            choicesArray.add(choiceNode);
            rootNode.set("choices", choicesArray);

            if (!withoutResponse) {
                when(objectMapper.readTree(anyString())).thenReturn(rootNode);
            }

        } catch (JsonProcessingException e) {
            fail("Setup failed due to JsonProcessingException: " + e.getMessage());
        }
    }

    // --- Success Tests ---

    @Test
    @DisplayName("generateGreeting - Success: Valid response from LLM")
    void generateGreeting_Success() throws IOException, InterruptedException {
        this.setUp();
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"" + EXPECTED_GREETING + "\"}}]}");

        // Call the service method
        String greeting = llmWrapperService.generateGreeting(TEST_NAME, TEST_AGE);

        // Assertions
        assertNotNull(greeting);
        assertEquals(EXPECTED_GREETING, greeting);

        // Verify interactions
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(objectMapper, times(1)).writeValueAsString(any()); // For LLMRequest serialization
        verify(objectMapper, times(1)).readTree(anyString()); // For response parsing
        verify(httpResponse, atLeastOnce()).statusCode(); // Called to check status
        verify(httpResponse, atLeastOnce()).body(); // Called to get body
    }

    // --- Negative Tests ---

    @Test
    @DisplayName("generateGreeting - Negative: Invalid LLM API URL configured")
    void generateGreeting_InvalidUrlConfig() throws IOException, InterruptedException {
        // Set an invalid URL to trigger URISyntaxException
        ReflectionTestUtils.setField(llmWrapperService, "url", "invalid-url with space");

        LLMServiceException exception = assertThrows(LLMServiceException.class,
                () -> llmWrapperService.generateGreeting(TEST_NAME, TEST_AGE));

        assertTrue(exception.getMessage().contains("Invalid LLM API URL configuration."));
        verify(httpClient, never()).send(any(), any()); // No HTTP call should be made
    }

    @Test
    @DisplayName("generateGreeting - Negative: JsonProcessingException during request serialization")
    void generateGreeting_JsonProcessingExceptionOnRequest() throws IOException, InterruptedException {
        setUp(true);
        when(objectMapper.writeValueAsString(any(LLMRequest.class))).thenThrow(new JsonProcessingException("Test JSON write error") {
        });

        LLMServiceException exception = assertThrows(LLMServiceException.class,
                () -> llmWrapperService.generateGreeting(TEST_NAME, TEST_AGE));

        assertTrue(exception.getMessage().contains("Error processing JSON with LLM service."));
        verify(httpClient, never()).send(any(), any()); // No HTTP call should be made
    }

    @Test
    @DisplayName("generateGreeting - Negative: IOException during HTTP call")
    void generateGreeting_IOExceptionOnHttpCall() throws IOException, InterruptedException {
        setUp(true);
        // Mock HttpClient.send to throw an IOException
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Network down"));

        LLMServiceException exception = assertThrows(LLMServiceException.class,
                () -> llmWrapperService.generateGreeting(TEST_NAME, TEST_AGE));

        assertTrue(exception.getMessage().contains("Network or I/O error when communicating with LLM service."));
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("generateGreeting - Negative: InterruptedException during HTTP call")
    void generateGreeting_InterruptedExceptionOnHttpCall() throws IOException, InterruptedException {
        setUp(true);
        // Mock HttpClient.send to throw an InterruptedException
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Call interrupted"));

        LLMServiceException exception = assertThrows(LLMServiceException.class,
                () -> llmWrapperService.generateGreeting(TEST_NAME, TEST_AGE));

        assertTrue(exception.getMessage().contains("LLM API call was interrupted."));
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        assertTrue(Thread.interrupted()); // Verify that the interrupt status is restored
    }

    @Test
    @DisplayName("generateGreeting - Negative: LLM API returned null response")
    void generateGreeting_NullResponse() throws IOException, InterruptedException {
        setUp(true);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(null);

        LLMServiceException exception = assertThrows(LLMServiceException.class,
                () -> llmWrapperService.generateGreeting(TEST_NAME, TEST_AGE));

        assertTrue(exception.getMessage().contains("LLM API returned an empty or null response body."));
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("generateGreeting - Negative: LLM API returned null response body")
    void generateGreeting_NullResponseBody() throws IOException, InterruptedException {
        setUp(true);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.body()).thenReturn(null);

        LLMServiceException exception = assertThrows(LLMServiceException.class,
                () -> llmWrapperService.generateGreeting(TEST_NAME, TEST_AGE));

        assertTrue(exception.getMessage().contains("LLM API returned an empty or null response body."));
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("generateGreeting - Negative: LLM API returned empty response body")
    void generateGreeting_EmptyResponseBody() throws IOException, InterruptedException {
        setUp(true);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.body()).thenReturn("");

        LLMServiceException exception = assertThrows(LLMServiceException.class,
                () -> llmWrapperService.generateGreeting(TEST_NAME, TEST_AGE));

        assertTrue(exception.getMessage().contains("LLM API returned an empty or null response body."));
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("generateGreeting - Negative: LLM API returned non-200 status code")
    void generateGreeting_Non200StatusCode() throws IOException, InterruptedException {
        setUp(true);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("{\"error\":\"Bad Request\"}"); // Example error body

        LLMServiceException exception = assertThrows(LLMServiceException.class,
                () -> llmWrapperService.generateGreeting(TEST_NAME, TEST_AGE));

        assertTrue(exception.getMessage().contains("LLM API call failed with status 400: {\"error\":\"Bad Request\"}"));
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(httpResponse, atLeastOnce()).statusCode();
        verify(httpResponse, atLeastOnce()).body();
    }

    @Test
    @DisplayName("generateGreeting - Negative: JsonProcessingException during response parsing")
    void generateGreeting_JsonProcessingExceptionOnResponse() throws IOException, InterruptedException {
        setUp(true);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("This is not JSON");

        // Mock ObjectMapper.readTree to throw an exception when parsing the malformed JSON
        when(objectMapper.readTree(anyString())).thenThrow(new JsonProcessingException("Malformed JSON") {
        });

        LLMServiceException exception = assertThrows(LLMServiceException.class,
                () -> llmWrapperService.generateGreeting(TEST_NAME, TEST_AGE));

        assertTrue(exception.getMessage().contains("Error processing JSON with LLM service."));
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(objectMapper, times(1)).readTree(anyString());
    }

    @Test
    @DisplayName("generateGreeting - Negative: LLM response missing 'choices' array")
    void generateGreeting_MissingChoicesArray() throws IOException, InterruptedException {
        setUp(true);
        // Mock ObjectMapper to return JSON without the "choices" field
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode(); // Empty root node
        when(objectMapper.readTree(anyString())).thenReturn(rootNode);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{}"); // Simple empty JSON

        LLMServiceException exception = assertThrows(LLMServiceException.class,
                () -> llmWrapperService.generateGreeting(TEST_NAME, TEST_AGE));

        assertTrue(exception.getMessage().contains("LLM response structure is unexpected: 'choices' array not found or empty."));
        verify(objectMapper, times(1)).readTree(anyString());
    }

    @Test
    @DisplayName("generateGreeting - Negative: LLM response with empty 'choices' array")
    void generateGreeting_EmptyChoicesArray() throws IOException, InterruptedException {
        setUp(true);
        // Mock ObjectMapper to return JSON with an empty "choices" array
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.set("choices", JsonNodeFactory.instance.arrayNode()); // Empty array
        when(objectMapper.readTree(anyString())).thenReturn(rootNode);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"choices\":[]}");

        LLMServiceException exception = assertThrows(LLMServiceException.class,
                () -> llmWrapperService.generateGreeting(TEST_NAME, TEST_AGE));

        assertTrue(exception.getMessage().contains("LLM response structure is unexpected: 'choices' array not found or empty."));
        verify(objectMapper, times(1)).readTree(anyString());
    }

    @Test
    @DisplayName("generateGreeting - Negative: General unexpected Exception")
    void generateGreeting_GeneralException() throws IOException, InterruptedException {
        setUp(true);
        // Mock HttpClient to throw a generic RuntimeException (or any unexpected exception)
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("An unexpected internal error"));

        LLMServiceException exception = assertThrows(LLMServiceException.class,
                () -> llmWrapperService.generateGreeting(TEST_NAME, TEST_AGE));

        assertTrue(exception.getMessage().contains("An unexpected error occurred while generating greeting."));
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}
