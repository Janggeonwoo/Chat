package com.example.chat.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB 내부에서 관리하는 순번 (1, 2, 3...)

    @Column(unique = true, nullable = false)
    private String userKey; // 기기 고유 UUID (프론트 localStorage에 저장될 비밀 키)

    @Column(nullable = false)
    private String nickname; // 유저가 입력한 닉네임

    private LocalDateTime createdAt; // 가입 일시

    // JPA를 위한 기본 생성자
    public User() {}

    // 가입할 때 쓸 생성자
    public User(String userKey, String nickname) {
        this.userKey = userKey;
        this.nickname = nickname;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getUserKey() { return userKey; }
    public String getNickname() { return nickname; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // 닉네임 변경 기능 대비용 Setter
    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }
}