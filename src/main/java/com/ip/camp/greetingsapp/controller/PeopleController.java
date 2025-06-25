package com.ip.camp.greetingsapp.controller;

import com.ip.camp.greetingsapp.dto.IdResult;
import com.ip.camp.greetingsapp.dto.MessageResponse;
import com.ip.camp.greetingsapp.entity.People;
import com.ip.camp.greetingsapp.service.PeopleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/people")
@Tag(name = "People Management", description = "APIs for managing people records and generating personalized greetings.")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class PeopleController {

    private final PeopleService peopleService;

    public PeopleController(PeopleService peopleService) {
        this.peopleService = peopleService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new person", description = "Creates a new person record in the database with provided details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Person created successfully, returns the ID of the created person",
                    content = @Content(schema = @Schema(type = "integer", format = "int64", description = "ID of the newly created person"))),
            @ApiResponse(responseCode = "400", description = "Bad request, e.g., invalid input data (null/empty name, age out of range)",
                    content = @Content(schema = @Schema(example = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Error details\"}")))
    })
    public ResponseEntity<IdResult> createPeople(@RequestBody People people) {
        Long peopleId = peopleService.createPeople(people);
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResult(peopleId));
    }

    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a person by ID", description = "Retrieves a specific person's details based on their unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Person found and returned successfully",
                    content = @Content(schema = @Schema(implementation = People.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, e.g., null ID provided",
                    content = @Content(schema = @Schema(example = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"No id was provided!\"}"))),
            @ApiResponse(responseCode = "404", description = "Person not found for the given ID",
                    content = @Content(schema = @Schema(example = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"People not found by id: 123\"}")))
    })
    public ResponseEntity<People> getPeopleById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(peopleService.getPeople(id));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all people", description = "Retrieves a list of all person records available in the database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all people returned successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = People.class))))
    })
    public ResponseEntity<Page<People>> getAllPeople(@ParameterObject Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(peopleService.getAllPeople(pageable));
    }

    @PutMapping(path = "{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update an existing person", description = "Updates an existing person's details identified by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Person updated successfully",
                    content = @Content(schema = @Schema(implementation = People.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, e.g., null ID, invalid input data",
                    content = @Content(schema = @Schema(example = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Error details\"}"))),
            @ApiResponse(responseCode = "404", description = "Person not found for the given ID",
                    content = @Content(schema = @Schema(example = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"The people not found by id: 123\"}")))
    })
    public ResponseEntity<People> updatePeople(@PathVariable Long id, @RequestBody People people) {
        People updatePeople = peopleService.updatePeople(id, people);
        return ResponseEntity.status(HttpStatus.OK).body(updatePeople);
    }

    @DeleteMapping(path = "{id}")
    @Operation(summary = "Delete a person by ID", description = "Deletes a specific person record based on their unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Person deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request, e.g., null ID provided",
                    content = @Content(schema = @Schema(example = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"No id was provided!\"}")))
    })
    public ResponseEntity<Object> deletePeople(@PathVariable Long id) {
        peopleService.deletePeople(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping(path = "{id}/greeting", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Generate a greeting for a person", description = "Generates a personalized greeting message for a person based on their ID using an LLM service.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Greeting generated successfully",
                    content = @Content(schema = @Schema(type = "string", example = "Hello, John Doe! You are 30 years old."))),
            @ApiResponse(responseCode = "400", description = "Bad request, e.g., null ID provided",
                    content = @Content(schema = @Schema(example = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"No id was provided!\"}"))),
            @ApiResponse(responseCode = "404", description = "Person not found for the given ID",
                    content = @Content(schema = @Schema(example = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"The people not found by id: 123\"}")))
    })
    public ResponseEntity<MessageResponse> generateGreeting(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(new MessageResponse(peopleService.generateGreeting(id)));
    }
}
