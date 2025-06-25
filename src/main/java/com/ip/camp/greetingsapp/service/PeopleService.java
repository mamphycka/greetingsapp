package com.ip.camp.greetingsapp.service;

import com.ip.camp.greetingsapp.entity.People;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface for basic CRUD operations for the People entity
 */

public interface PeopleService {

    Long createPeople(People people);

    People getPeople(Long id);

    People updatePeople(Long id, People people);

    void deletePeople(Long id);

    Page<People> getAllPeople(Pageable pageable);

    String generateGreeting(Long id);

}
