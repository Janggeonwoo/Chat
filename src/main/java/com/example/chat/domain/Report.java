package com.example.chat.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🚨 신고를 한 유저의 키
    @Column(nullable = false)
    private String reporterKey;

    // 🎯 신고를 당한 악성 유저의 키 (Target)
    @Column(nullable = false)
    private String targetKey;

    // 사유 기록용 (예: 욕설, 음란물 등 확장성 대비)
    private String reason;

    private LocalDateTime createdAt;

    public Report() {}

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // ─── Getter / Setter ───
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReporterKey() { return reporterKey; }
    public void setReporterKey(String reporterKey) { this.reporterKey = reporterKey; }

    public String getTargetKey() { return targetKey; }
    public void setTargetKey(String targetKey) { this.targetKey = targetKey; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}