package com.ip.camp.greetingsapp.repository;

import com.ip.camp.greetingsapp.entity.People;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeopleRepository extends JpaRepository<People, Long> {
}
