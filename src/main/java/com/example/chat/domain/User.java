package com.example.chat.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String userKey;

    @Column(nullable = false)
    private String nickname;

    @Column(length = 10)
    private String gender; // "MALE" 또는 "FEMALE"

    @Column(length = 20)
    private String ageRange; // "20~29" 형태

    private Integer age; // 💡 숫자로 된 나이 컬럼 추가!

    private LocalDateTime createdAt;

    public User() {}

    // 생성자 보정
    public User(String userKey, String nickname, String gender, String ageRange, Integer age) {
        this.userKey = userKey;
        this.nickname = nickname;
        this.gender = gender;
        this.ageRange = ageRange;
        this.age = age;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getUserKey() { return userKey; }
    public String getNickname() { return nickname; }
    public String getGender() { return gender; }
    public String getAgeRange() { return ageRange; }
    public Integer getAge() { return age; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // 정보 업데이트 메서드 보정
    public void updateInfo(String gender, String ageRange, Integer age) {
        this.gender = gender;
        this.ageRange = ageRange;
        this.age = age;
    }
}