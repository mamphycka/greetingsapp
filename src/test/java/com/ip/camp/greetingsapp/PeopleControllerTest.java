package com.ip.camp.greetingsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ip.camp.greetingsapp.controller.PeopleController;
import com.ip.camp.greetingsapp.entity.Gender;
import com.ip.camp.greetingsapp.entity.People;
import com.ip.camp.greetingsapp.exception.BadRequestException;
import com.ip.camp.greetingsapp.exception.NotFoundException;
import com.ip.camp.greetingsapp.service.PeopleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PeopleController.class) // Focuses on testing the web layer
class PeopleControllerTest {

    @Autowired
    private MockMvc mockMvc; // Used to send HTTP requests to the controller

    @Autowired
    private ObjectMapper objectMapper; // Helps convert Java objects to JSON and vice versa

    @MockitoBean
    private PeopleService peopleService;

    private People samplePeople;

    // Set up a common People object before each test
    @BeforeEach
    void setUp() {
        samplePeople = new People();
        samplePeople.setId(1L);
        samplePeople.setName("John Doe");
        samplePeople.setAge(30);
        samplePeople.setGender(Gender.MALE);
        samplePeople.setKeywords(List.of("Friendly", "Tech-savvy"));
    }

    // --- createPeople Tests ---
    @Test
    @DisplayName("POST /api/v1/people - Success: Create People")
    void createPeople_Success() throws Exception {
        // Mock the service layer call to return a new ID
        when(peopleService.createPeople(any(People.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/people")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePeople))) // Convert People object to JSON string
                .andExpect(status().isCreated()) // Expect HTTP 201 Created
                .andExpect(jsonPath("$.id").value(1)); // Expect the returned ID as a string

        // Verify that the service method was called once with any People object
        verify(peopleService, times(1)).createPeople(any(People.class));
    }

    @Test
    @DisplayName("POST /api/v1/people - Negative: Bad Request on Create (e.g., invalid age)")
    void createPeople_BadRequest() throws Exception {
        // Simulate a BadRequestException from the service layer
        doThrow(new BadRequestException("Age must be between 0 and 126!"))
                .when(peopleService).createPeople(any(People.class));

        // Create a people object that would cause a bad request
        People invalidPeople = new People();
        invalidPeople.setName("Invalid Age");
        invalidPeople.setAge(200);

        mockMvc.perform(post("/api/v1/people")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPeople)))
                .andExpect(status().isBadRequest()) // Expect HTTP 400 Bad Request
                .andExpect(jsonPath("$.message").value("Age must be between 0 and 126!")); // Check error message

        verify(peopleService, times(1)).createPeople(any(People.class));
    }

    // --- getPeopleById Tests ---
    @Test
    @DisplayName("GET /api/v1/people/{id} - Success: Get People by ID")
    void getPeopleById_Success() throws Exception {
        // Mock the service layer call to return the sample People object
        when(peopleService.getPeople(samplePeople.getId())).thenReturn(samplePeople);

        mockMvc.perform(get("/api/v1/people/{id}", samplePeople.getId()))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(jsonPath("$.id").value(samplePeople.getId()))
                .andExpect(jsonPath("$.name").value(samplePeople.getName()))
                .andExpect(jsonPath("$.age").value(samplePeople.getAge()));

        verify(peopleService, times(1)).getPeople(samplePeople.getId());
    }

    @Test
    @DisplayName("GET /api/v1/people/{id} - Negative: Not Found")
    void getPeopleById_NotFound() throws Exception {
        // Simulate a NotFoundException from the service layer
        doThrow(new NotFoundException("People not found by id: 99"))
                .when(peopleService).getPeople(anyLong());

        mockMvc.perform(get("/api/v1/people/{id}", 99L))
                .andExpect(status().isNotFound()) // Expect HTTP 404 Not Found
                .andExpect(jsonPath("$.message").value("People not found by id: 99"));

        verify(peopleService, times(1)).getPeople(99L);
    }


    // --- getAllPeople Tests ---
    @Test
    @DisplayName("GET /api/v1/people - Success: Get All People")
    void getAllPeople_Success() throws Exception {
        People anotherPeople = new People();
        anotherPeople.setId(2L);
        anotherPeople.setName("Jane Doe");
        anotherPeople.setAge(25);

        Page<People> peopleList = new PageImpl<>(Arrays.asList(samplePeople, anotherPeople));
        // Mock the service layer call to return a list of People
        when(peopleService.getAllPeople(any(Pageable.class))).thenReturn(peopleList);

        mockMvc.perform(get("/api/v1/people").param("page", "5")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(jsonPath("$.content.length()").value(2)) // Expect 2 elements in the JSON array
                .andExpect(jsonPath("$.content.[0].id").value(samplePeople.getId()))
                .andExpect(jsonPath("$.content.[1].name").value(anotherPeople.getName()));

        verify(peopleService, times(1)).getAllPeople(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/v1/people - Success: Get All People (Empty List)")
    void getAllPeople_EmptyList() throws Exception {
        // Mock the service layer call to return an empty list
        when(peopleService.getAllPeople(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/people"))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(jsonPath("$.content.length()").value(0)); // Expect 0 elements in the JSON array

        verify(peopleService, times(1)).getAllPeople(any(Pageable.class));
    }

    // --- updatePeople Tests ---
    @Test
    @DisplayName("PUT /api/v1/people/{id} - Success: Update People")
    void updatePeople_Success() throws Exception {
        People updatedDetails = new People();
        updatedDetails.setName("John Updated");
        updatedDetails.setAge(31);
        updatedDetails.setGender(Gender.MALE);
        updatedDetails.setKeywords(List.of("Updated Keywords"));

        // The service should return the updated People object
        when(peopleService.updatePeople(anyLong(), any(People.class))).thenReturn(samplePeople);

        mockMvc.perform(put("/api/v1/people/{id}", samplePeople.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(jsonPath("$.id").value(samplePeople.getId()))
                .andExpect(jsonPath("$.name").value(samplePeople.getName())) // Verify the returned object's name
                .andExpect(jsonPath("$.age").value(samplePeople.getAge()));

        verify(peopleService, times(1)).updatePeople(eq(samplePeople.getId()), any(People.class));
    }

    @Test
    @DisplayName("PUT /api/v1/people/{id} - Negative: Not Found on Update")
    void updatePeople_NotFound() throws Exception {
        // Simulate a NotFoundException when updating a non-existent ID
        doThrow(new NotFoundException("The people not found by id: 99"))
                .when(peopleService).updatePeople(anyLong(), any(People.class));

        mockMvc.perform(put("/api/v1/people/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePeople)))
                .andExpect(status().isNotFound()) // Expect HTTP 404 Not Found
                .andExpect(jsonPath("$.message").value("The people not found by id: 99"));

        verify(peopleService, times(1)).updatePeople(eq(99L), any(People.class));
    }

    @Test
    @DisplayName("PUT /api/v1/people/{id} - Negative: Bad Request on Update (e.g., invalid input)")
    void updatePeople_BadRequest() throws Exception {
        // Simulate a BadRequestException from the service layer
        doThrow(new BadRequestException("Name cannot be null or empty!"))
                .when(peopleService).updatePeople(anyLong(), any(People.class));

        People invalidUpdate = new People();
        invalidUpdate.setAge(30);
        invalidUpdate.setName(""); // Invalid name

        mockMvc.perform(put("/api/v1/people/{id}", samplePeople.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdate)))
                .andExpect(status().isBadRequest()) // Expect HTTP 400 Bad Request
                .andExpect(jsonPath("$.message").value("Name cannot be null or empty!"));

        verify(peopleService, times(1)).updatePeople(eq(samplePeople.getId()), any(People.class));
    }

    // --- deletePeople Tests ---
    @Test
    @DisplayName("DELETE /api/v1/people/{id} - Success: Delete People")
    void deletePeople_Success() throws Exception {
        // Mock the service method to do nothing (successful deletion)
        doNothing().when(peopleService).deletePeople(samplePeople.getId());

        mockMvc.perform(delete("/api/v1/people/{id}", samplePeople.getId()))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(content().string("")); // Expect no content in the response body

        verify(peopleService, times(1)).deletePeople(samplePeople.getId());
    }

    // --- generateGreeting Tests ---
    @Test
    @DisplayName("POST /api/v1/people/{id}/greeting - Success: Generate Greeting")
    void generateGreeting_Success() throws Exception {
        String expectedGreeting = "Hello John Doe, you are 30 years old!";
        // Mock the service call to return a greeting string
        when(peopleService.generateGreeting(samplePeople.getId())).thenReturn(expectedGreeting);

        mockMvc.perform(post("/api/v1/people/{id}/greeting", samplePeople.getId()))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(jsonPath("$.message").value(expectedGreeting)); // Expect the specific greeting string

        verify(peopleService, times(1)).generateGreeting(samplePeople.getId());
    }

    @Test
    @DisplayName("POST /api/v1/people/{id}/greeting - Negative: Not Found on Greeting Generation")
    void generateGreeting_NotFound() throws Exception {
        // Simulate NotFoundException from the service
        doThrow(new NotFoundException("The people not found by id: 99"))
                .when(peopleService).generateGreeting(anyLong());

        mockMvc.perform(post("/api/v1/people/{id}/greeting", 99L))
                .andExpect(status().isNotFound()) // Expect HTTP 404 Not Found
                .andExpect(jsonPath("$.message").value("The people not found by id: 99"));

        verify(peopleService, times(1)).generateGreeting(99L);
    }
}
