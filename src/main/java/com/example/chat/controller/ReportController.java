package com.example.chat.controller;

import com.example.chat.domain.Report;
import com.example.chat.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

// 🔄 ReportController.java 파일 상단 애노테이션 구역 수정

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
// 🔥 allowedHeaders, methods, maxAge까지 짱짱하게 채워 Preflight 차단을 완전히 박멸합니다.
@CrossOrigin(
        origins = "http://localhost:5173",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        maxAge = 3600
)
public class ReportController {

    private final ReportRepository reportRepository;

    /**
     * 🚨 1. 악성 유저 신고 접수 API
     */
    @PostMapping("/submit")
    public ResponseEntity<String> submitReport(@RequestBody Map<String, String> payload) {
        String reporterKey = payload.get("reporterKey");
        String targetKey = payload.get("targetKey");
        String reason = payload.get("reason");

        if (reporterKey == null || targetKey == null) {
            return new ResponseEntity<>("잘못된 유저 정보입니다.", HttpStatus.BAD_REQUEST);
        }

        if (reporterKey.equals(targetKey)) {
            return new ResponseEntity<>("자기 자신을 신고할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        // 중복 신고 방어
//        Optional<Report> alreadyReported = reportRepository.findByReporterKeyAndTargetKey(reporterKey, targetKey);
//        if (alreadyReported.isPresent()) {
//            return new ResponseEntity<>("이미 이 유저를 신고하셨습니다.", HttpStatus.CONFLICT);
//        }

        // 신고 데이터 적재
        Report report = new Report();
        report.setReporterKey(reporterKey);
        report.setTargetKey(targetKey);
        report.setReason(reason != null ? reason : "일반 신고");
        reportRepository.save(report);

        return new ResponseEntity<>("신고가 정상적으로 접수되었습니다.", HttpStatus.OK);
    }

    /**
     * 🛡️ 2. 이 유저가 블락(밴) 대상인지 확인하는 API (매칭 시작 전 제어용)
     */
    @GetMapping("/check-ban/{userKey}")
    public ResponseEntity<Map<String, Object>> checkBanStatus(@PathVariable String userKey) {
        // 당한 신고 횟수 카운트
        long reportCount = reportRepository.countByTargetKey(userKey);

        // 💡 기준점: 누적 3회 이상이면 이용 정지(isBanned = true)
        boolean isBanned = reportCount >= 1;

        Map<String, Object> response = Map.of(
                "isBanned", isBanned,
                "reportCount", reportCount
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 🛠️ [운영자] 3. 현재 접수된 모든 신고 내역 조회 API (최신순)
     */
    @GetMapping("/admin/list")
    public ResponseEntity<java.util.List<Report>> getAllReports() {
        // 최신 신고가 맨 위로 오도록 정렬하여 반환
        java.util.List<Report> reports = reportRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );
        return new ResponseEntity<>(reports, HttpStatus.OK);
    }

    /**
     * 🛠️ [운영자] 4. 특정 유저 정지 해제 API (신고 스택 초기화)
     */
    @Transactional // @org.springframework.transaction.annotation.Transactional 추가 필수
    @DeleteMapping("/admin/unban/{userKey}")
    public ResponseEntity<String> unbanUser(@PathVariable String userKey) {
        // 💡 백엔드 전용 Repository에 커스텀 메서드를 써도 되지만,
        // 여기서는 간단하게 해당 유저(targetKey)가 받은 모든 신고 내역을 찾아 지웁니다.
        // (JPA 기본 제공 메서드로 처리하기 위해 Report 엔티티 리스트를 받아 한 번에 지우는 방식)

        // 이 작업을 위해 ReportRepository에 단 한 줄 추가가 필요할 수 있으니 아래 2단계를 확인하세요!
        try {
            reportRepository.deleteByTargetKey(userKey);
            return new ResponseEntity<>("해당 유저의 모든 신고 스택이 파기되어 정지가 해제되었습니다.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("정지 해제 처리 중 오류 발생", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}