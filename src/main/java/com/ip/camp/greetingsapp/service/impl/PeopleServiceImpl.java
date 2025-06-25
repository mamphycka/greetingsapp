package com.ip.camp.greetingsapp.service.impl;

import com.ip.camp.greetingsapp.entity.People;
import com.ip.camp.greetingsapp.exception.BadRequestException;
import com.ip.camp.greetingsapp.exception.NotFoundException;
import com.ip.camp.greetingsapp.repository.PeopleRepository;
import com.ip.camp.greetingsapp.service.LLMWrapperService;
import com.ip.camp.greetingsapp.service.PeopleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PeopleServiceImpl implements PeopleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeopleServiceImpl.class);

    private final PeopleRepository peopleRepository;
    private final LLMWrapperService llmWrapperService;

    public PeopleServiceImpl(PeopleRepository peopleRepository, LLMWrapperService llmWrapperService) {
        this.peopleRepository = peopleRepository;
        this.llmWrapperService = llmWrapperService;
    }

    @Override
    public Long createPeople(People people) {
        validatePeople(people);
        People storedPeople = peopleRepository.save(people);
        LOGGER.debug("Created new people: {}", storedPeople);
        return storedPeople.getId();
    }

    @Override
    public People getPeople(Long id) {
        if (id == null) {
            throw new BadRequestException("No id was provided!");
        }
        Optional<People> people = peopleRepository.findById(id);
        if (people.isEmpty()) {
            throw new NotFoundException("People not found by id: " + id);
        }
        return people.get();
    }

    @Override
    public People updatePeople(Long id, People people) {

        validatePeople(people);

        if (id == null) {
            throw new BadRequestException("No id was provided!");
        }
        Optional<People> optionalPeople = peopleRepository.findById(id);
        if (optionalPeople.isEmpty()) {
            throw new NotFoundException("The people not found by id: " + id);
        }
        People loadedPeople = optionalPeople.get();
        loadedPeople.setAge(people.getAge());
        if (people.getName() != null) {
            loadedPeople.setName(people.getName());
        }
        if (people.getGender() != null) {
            loadedPeople.setGender(people.getGender());
        }
        if (people.getKeywords() != null) {
            loadedPeople.setKeywords(people.getKeywords());
        }
        loadedPeople = peopleRepository.save(loadedPeople);
        LOGGER.debug("Updated people: {}", loadedPeople);
        return loadedPeople;
    }

    @Override
    public void deletePeople(Long id) {
        if (id == null) {
            throw new BadRequestException("No id was provided!");
        }
        peopleRepository.deleteById(id);
        LOGGER.debug("Removed people by id: {}", id);
    }

    @Override
    public Page<People> getAllPeople(Pageable pageable) {
        return peopleRepository.findAll(pageable);
    }

    @Override
    public String generateGreeting(Long id) {
        if (id == null) {
            throw new BadRequestException("No id was provided!");
        }
        Optional<People> optionalPeople = peopleRepository.findById(id);
        if (optionalPeople.isEmpty()) {
            throw new NotFoundException("The people not found by id: " + id);
        }
        People loadedPeople = optionalPeople.get();
        return llmWrapperService.generateGreeting(loadedPeople.getName(), loadedPeople.getAge());
    }

    /**
     * Validating the people request object
     *
     * @param people - People request to be validated
     */
    private void validatePeople(People people) {
        if (people == null) {
            throw new BadRequestException("The people object is null!");
        }
        if (people.getAge() < 0 || people.getAge() > 126) {
            throw new BadRequestException("Age must be between 0 and 126!");
        }
        if (people.getName() == null || people.getName().isEmpty()) {
            throw new BadRequestException("Name cannot be null or empty!");
        }
        if (people.getGender() == null) {
            throw new BadRequestException("Gender cannot be null!");
        }
    }
}
