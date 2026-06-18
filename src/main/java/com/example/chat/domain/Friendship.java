package com.example.chat.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "friendships")
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🙋‍♂️ 친구 신청을 보낸 사람의 고유 기기 키 (UUID)
    @Column(nullable = false)
    private String requesterKey;

    // 🙋‍♀️ 친구 신청을 받은 사람의 고유 기기 키 (UUID)
    @Column(nullable = false)
    private String receiverKey;

    // 닉네임이 매번 바뀔 수 있으므로, 신청 당시 혹은 현재 시점의 서로를 식별할 임시 닉네임 기록
    private String requesterNickname;
    private String receiverNickname;

    // 🚦 친구 상태 관리를 위한 변수 (PENDING: 대기중, ACCEPTED: 친구 완료)
    @Column(nullable = false)
    private String status = "PENDING";

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Friendship() {}

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Getter / Setter 세트 ───
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequesterKey() { return requesterKey; }
    public void setRequesterKey(String requesterKey) { this.requesterKey = requesterKey; }

    public String getReceiverKey() { return receiverKey; }
    public void setReceiverKey(String receiverKey) { this.receiverKey = receiverKey; }

    public String getRequesterNickname() { return requesterNickname; }
    public void setRequesterNickname(String requesterNickname) { this.requesterNickname = requesterNickname; }

    public String getReceiverNickname() { return receiverNickname; }
    public void setReceiverNickname(String receiverNickname) { this.receiverNickname = receiverNickname; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}