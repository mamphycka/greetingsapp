package com.ip.camp.greetingsapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity(name = "People")
@Table(name = "people")
public class People implements Serializable {

    @Serial
    private static final long serialVersionUID = 1;

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private int age;
    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;
    private List<String> keywords;
    @Version
    @JsonIgnore
    private int version;
    @CreationTimestamp
    private LocalDateTime createdDate;
    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    public People() {
    }

    public People(String name, byte age, Gender gender, List<String> keywords) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.keywords = keywords;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public String toString() {
        return "People{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", gender=" + gender +
                ", keywords=" + keywords +
                ", version=" + version +
                ", createdDate=" + createdDate +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        People people = (People) o;
        return age == people.age && version == people.version && Objects.equals(id, people.id) && Objects.equals(name, people.name)
                && gender == people.gender && Objects.equals(keywords, people.keywords) && Objects.equals(createdDate, people.createdDate)
                && Objects.equals(lastUpdated, people.lastUpdated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, age, gender, keywords, version, createdDate, lastUpdated);
    }
}
