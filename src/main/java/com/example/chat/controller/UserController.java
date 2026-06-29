package com.example.chat.controller;

import com.example.chat.domain.User;
import com.example.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
public class UserController {

    private final UserService userService;

    /**
     * ─── ⚡ [자동 로그인 주소] 앱 재접속 시 로컬 스토리지의 userKey 검증 ───
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String userKey = request.get("userKey");
        String nickname = request.get("nickname");

        // 💡 [수정 완료] 기존 4개 인자 뒤에 ', null'을 하나 더 추가해서 총 5개의 인자 규격을 맞춥니다.
        User user = userService.loginOrRegister(userKey, nickname, null, null, null);

        Map<String, Object> response = new HashMap<>();
        response.put("userKey", user.getUserKey());
        response.put("nickname", user.getNickname());
        response.put("gender", user.getGender());

        return ResponseEntity.ok(response);
    }

    /**
     * ─── ✍️ [프로필 수동 저장 API] ───
     * 카카오 가입 직후 프론트에서 직접 고른 성별, 나이, 연령대를 받아 DB에 최종 낙점합니다.
     */
    @PostMapping("/update-profile")
    public ResponseEntity<String> updateProfile(@RequestBody Map<String, Object> request) {
        try {
            String userKey = (String) request.get("userKey");
            String gender = (String) request.get("gender");

            // 나이(출생연도) 숫자가 정수형태로 안전하게 오는지 체크 후 캐스팅
            Integer age = null;
            if (request.get("age") != null) {
                age = Integer.parseInt(String.valueOf(request.get("age")));
            }
            String ageRange = (String) request.get("ageRange");

            // 🎯 [수정 완료] 5번째 인자에 진짜 나이(출생연도) 변수인 'age'를 정확히 수록해 줍니다!
            // 인자 순서: userKey, nickname, gender, ageRange, age
            User user = userService.loginOrRegister(userKey, null, gender, ageRange, age);

            System.out.println("✅ [DB 마크 완료] 유저 수동 프로필 확정 -> Key: " + userKey + " | 성별: " + gender + " | 출생 연도: " + age + " | 연령대 대역: " + ageRange);
            return ResponseEntity.ok("Profile updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("프로필 저장 중 서버 오류 발생");
        }
    }
}