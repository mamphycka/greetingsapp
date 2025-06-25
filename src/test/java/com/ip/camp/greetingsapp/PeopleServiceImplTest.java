package com.ip.camp.greetingsapp;

import com.ip.camp.greetingsapp.entity.Gender;
import com.ip.camp.greetingsapp.entity.People;
import com.ip.camp.greetingsapp.exception.BadRequestException;
import com.ip.camp.greetingsapp.exception.NotFoundException;
import com.ip.camp.greetingsapp.repository.PeopleRepository;
import com.ip.camp.greetingsapp.service.LLMWrapperService;
import com.ip.camp.greetingsapp.service.impl.PeopleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeopleServiceImplTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10, Sort.by("id").ascending());
    @Mock
    private PeopleRepository peopleRepository;
    @Mock
    private LLMWrapperService llmWrapperService;
    @InjectMocks
    private PeopleServiceImpl peopleService;
    private People samplePeople;

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
    @DisplayName("createPeople - Success")
    void createPeople_Success() {
        when(peopleRepository.save(any(People.class))).thenReturn(samplePeople);

        Long createdId = peopleService.createPeople(samplePeople);

        assertNotNull(createdId);
        assertEquals(samplePeople.getId(), createdId);
        verify(peopleRepository, times(1)).save(samplePeople);
    }

    @Test
    @DisplayName("createPeople - Negative: Null People Object")
    void createPeople_Negative_NullPeopleObject() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> peopleService.createPeople(null));

        assertEquals("The people object is null!", exception.getMessage());
        verify(peopleRepository, never()).save(any(People.class));
    }

    @Test
    @DisplayName("createPeople - Negative: Invalid Age - Too Low")
    void createPeople_Negative_InvalidAgeTooLow() {
        samplePeople.setAge(-5); // Set invalid age

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> peopleService.createPeople(samplePeople));

        assertEquals("Age must be between 0 and 126!", exception.getMessage());
        verify(peopleRepository, never()).save(any(People.class));
    }

    @Test
    @DisplayName("createPeople - Negative: Invalid Age - Too High")
    void createPeople_Negative_InvalidAgeTooHigh() {
        samplePeople.setAge(150); // Set invalid age

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> peopleService.createPeople(samplePeople));

        assertEquals("Age must be between 0 and 126!", exception.getMessage());
        verify(peopleRepository, never()).save(any(People.class));
    }

    @Test
    @DisplayName("createPeople - Negative: Null Name")
    void createPeople_Negative_NullName() {
        samplePeople.setName(null); // Set null name

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> peopleService.createPeople(samplePeople));

        assertEquals("Name cannot be null or empty!", exception.getMessage());
        verify(peopleRepository, never()).save(any(People.class));
    }

    @Test
    @DisplayName("createPeople - Negative: Empty Name")
    void createPeople_Negative_EmptyName() {
        samplePeople.setName(""); // Set empty name

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> peopleService.createPeople(samplePeople));

        assertEquals("Name cannot be null or empty!", exception.getMessage());
        verify(peopleRepository, never()).save(any(People.class));
    }

    // --- getPeople Tests ---
    @Test
    @DisplayName("getPeople - Success")
    void getPeople_Success() {
        when(peopleRepository.findById(samplePeople.getId())).thenReturn(Optional.of(samplePeople));

        People foundPeople = peopleService.getPeople(samplePeople.getId());

        assertNotNull(foundPeople);
        assertEquals(samplePeople.getId(), foundPeople.getId());
        assertEquals(samplePeople.getName(), foundPeople.getName());
        verify(peopleRepository, times(1)).findById(samplePeople.getId());
    }

    @Test
    @DisplayName("getPeople - Negative: Null ID")
    void getPeople_Negative_NullId() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> peopleService.getPeople(null));

        assertEquals("No id was provided!", exception.getMessage());
        verify(peopleRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("getPeople - Negative: Not Found")
    void getPeople_Negative_NotFound() {
        when(peopleRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> peopleService.getPeople(99L));

        assertEquals("People not found by id: 99", exception.getMessage());
        verify(peopleRepository, times(1)).findById(99L);
    }

    // --- updatePeople Tests ---
    @Test
    @DisplayName("updatePeople - Success: All Fields Updated")
    void updatePeople_Success_AllFieldsUpdated() {
        People updatedPeopleDetails = new People();
        updatedPeopleDetails.setId(samplePeople.getId());
        updatedPeopleDetails.setName("Jane Doe");
        updatedPeopleDetails.setAge(35);
        updatedPeopleDetails.setGender(Gender.FEMALE);
        updatedPeopleDetails.setKeywords(List.of("Creative", "Artistic"));

        when(peopleRepository.findById(samplePeople.getId())).thenReturn(Optional.of(samplePeople));
        when(peopleRepository.save(any(People.class))).thenReturn(updatedPeopleDetails);

        People result = peopleService.updatePeople(samplePeople.getId(), updatedPeopleDetails);

        assertNotNull(result);
        assertEquals(samplePeople.getId(), result.getId());
        assertEquals(updatedPeopleDetails.getName(), result.getName());
        assertEquals(updatedPeopleDetails.getAge(), result.getAge());
        assertEquals(updatedPeopleDetails.getGender(), result.getGender());
        assertEquals(updatedPeopleDetails.getKeywords(), result.getKeywords());

        verify(peopleRepository, times(1)).findById(samplePeople.getId());
        verify(peopleRepository, times(1)).save(any(People.class));
    }

    @Test
    @DisplayName("updatePeople - Success: Partial Update (only name)")
    void updatePeople_Success_PartialUpdateName() {
        People updatedPeopleDetails = new People();
        updatedPeopleDetails.setName("New Name");
        updatedPeopleDetails.setAge(samplePeople.getAge());
        updatedPeopleDetails.setGender(samplePeople.getGender());

        when(peopleRepository.findById(samplePeople.getId())).thenReturn(Optional.of(samplePeople));
        when(peopleRepository.save(any(People.class))).thenAnswer(invocation -> {
            People savedPeople = invocation.getArgument(0);
            assertEquals("New Name", savedPeople.getName());
            assertEquals(samplePeople.getAge(), savedPeople.getAge()); // Age should be preserved
            assertEquals(samplePeople.getGender(), savedPeople.getGender()); // Gender should be preserved
            return savedPeople;
        });

        People result = peopleService.updatePeople(samplePeople.getId(), updatedPeopleDetails);

        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals(samplePeople.getAge(), result.getAge());
        verify(peopleRepository, times(1)).findById(samplePeople.getId());
        verify(peopleRepository, times(1)).save(any(People.class));
    }


    @Test
    @DisplayName("updatePeople - Negative: Null ID")
    void updatePeople_Negative_NullId() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> peopleService.updatePeople(null, samplePeople));

        assertEquals("No id was provided!", exception.getMessage());
        verify(peopleRepository, never()).findById(anyLong());
        verify(peopleRepository, never()).save(any(People.class));
    }

    @Test
    @DisplayName("updatePeople - Negative: People Not Found")
    void updatePeople_Negative_PeopleNotFound() {
        when(peopleRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> peopleService.updatePeople(99L, samplePeople));

        assertEquals("The people not found by id: 99", exception.getMessage());
        verify(peopleRepository, times(1)).findById(99L);
        verify(peopleRepository, never()).save(any(People.class));
    }

    @Test
    @DisplayName("updatePeople - Negative: Invalid Age in Update Object")
    void updatePeople_Negative_InvalidAgeInUpdateObject() {
        People invalidAgePeople = new People();
        invalidAgePeople.setName("Valid Name");
        invalidAgePeople.setAge(200); // Invalid age

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> peopleService.updatePeople(samplePeople.getId(), invalidAgePeople));

        assertEquals("Age must be between 0 and 126!", exception.getMessage());
        verify(peopleRepository, never()).findById(anyLong());
        verify(peopleRepository, never()).save(any(People.class));
    }

    @Test
    @DisplayName("updatePeople - Negative: Null Name in Update Object")
    void updatePeople_Negative_NullNameInUpdateObject() {
        People nullNamePeople = new People();
        nullNamePeople.setName(null);
        nullNamePeople.setAge(30);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> peopleService.updatePeople(samplePeople.getId(), nullNamePeople));

        assertEquals("Name cannot be null or empty!", exception.getMessage());
        verify(peopleRepository, never()).findById(anyLong());
        verify(peopleRepository, never()).save(any(People.class));
    }

    // --- deletePeople Tests ---
    @Test
    @DisplayName("deletePeople - Success")
    void deletePeople_Success() {
        doNothing().when(peopleRepository).deleteById(samplePeople.getId());

        peopleService.deletePeople(samplePeople.getId());

        verify(peopleRepository, times(1)).deleteById(samplePeople.getId());
    }

    @Test
    @DisplayName("deletePeople - Negative: Null ID")
    void deletePeople_Negative_NullId() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> peopleService.deletePeople(null));

        assertEquals("No id was provided!", exception.getMessage());
        verify(peopleRepository, never()).deleteById(anyLong());
    }

    // --- getAllPeople Tests ---
    @Test
    @DisplayName("getAllPeople - Success: Returns List of People")
    void getAllPeople_Success() {
        People anotherPeople = new People();
        anotherPeople.setId(2L);
        anotherPeople.setName("Jane Doe");
        anotherPeople.setAge(25);

        when(peopleRepository.findAll(PAGEABLE)).thenReturn(new PageImpl<>(Arrays.asList(samplePeople, anotherPeople)));
        Page<People> result = peopleService.getAllPeople(PAGEABLE);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().contains(samplePeople));
        assertTrue(result.getContent().contains(anotherPeople));
        verify(peopleRepository, times(1)).findAll(PAGEABLE);
    }

    @Test
    @DisplayName("getAllPeople - Success: Returns Empty List")
    void getAllPeople_Success_EmptyList() {
        when(peopleRepository.findAll(PAGEABLE)).thenReturn(Page.empty());

        Page<People> result = peopleService.getAllPeople(PAGEABLE);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(peopleRepository, times(1)).findAll(PAGEABLE);
    }

    // --- generateGreeting Tests ---
    @Test
    @DisplayName("generateGreeting - Success")
    void generateGreeting_Success() {
        String expectedGreeting = "Hello John Doe, you are 30 years old!";
        when(peopleRepository.findById(samplePeople.getId())).thenReturn(Optional.of(samplePeople));
        when(llmWrapperService.generateGreeting(samplePeople.getName(), samplePeople.getAge()))
                .thenReturn(expectedGreeting);

        String greeting = peopleService.generateGreeting(samplePeople.getId());

        assertNotNull(greeting);
        assertEquals(expectedGreeting, greeting);
        verify(peopleRepository, times(1)).findById(samplePeople.getId());
        verify(llmWrapperService, times(1)).generateGreeting(samplePeople.getName(), samplePeople.getAge());
    }

    @Test
    @DisplayName("generateGreeting - Negative: Null ID")
    void generateGreeting_Negative_NullId() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> peopleService.generateGreeting(null));

        assertEquals("No id was provided!", exception.getMessage());
        verify(peopleRepository, never()).findById(anyLong());
        verify(llmWrapperService, never()).generateGreeting(anyString(), anyInt());
    }

    @Test
    @DisplayName("generateGreeting - Negative: People Not Found")
    void generateGreeting_Negative_PeopleNotFound() {
        when(peopleRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> peopleService.generateGreeting(99L));

        assertEquals("The people not found by id: 99", exception.getMessage());
        verify(peopleRepository, times(1)).findById(99L);
        verify(llmWrapperService, never()).generateGreeting(anyString(), anyInt());
    }
}
