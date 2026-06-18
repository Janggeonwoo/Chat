package com.example.chat.repository;

import com.example.chat.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByReporterKeyAndTargetKey(String reporterKey, String targetKey);
    long countByTargetKey(String targetKey);

    // 🔥 [운영자용 쿼리 추가] 특정 유저가 당한 신고 데이터를 싹 밀어버리는 무기
    void deleteByTargetKey(String targetKey);
}