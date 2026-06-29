package com.example.chat.service;

import com.example.chat.repository.UserRepository;
import com.example.chat.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 🎲 카카오 및 수동 프로필 주입 로그인 및 가입
     * 💡 [수정] 5번째 인자로 진짜 나이(Integer age)를 직접 수신하도록 스펙을 확장합니다.
     */
    @Transactional
    public User loginOrRegister(String userKey, String nickname, String gender, String ageRange, Integer age) {

        // 1. 인자로 진짜 나이(age)가 안 넘어왔을 때만 (카카오 로그인 시점) 대역폭 앞글자 파싱 진행
        Integer parsedAge = age;
        if (parsedAge == null && ageRange != null && ageRange.contains("~")) {
            try {
                String ageStr = ageRange.split("~")[0]; // "20" 추출
                parsedAge = Integer.parseInt(ageStr);   // 숫자로 변경
            } catch (Exception e) {
                System.out.println("⚠️ 나이 파싱 실패: " + e.getMessage());
            }
        }

        final Integer finalAge = parsedAge;

        if (userKey != null && !userKey.trim().isEmpty()) {
            return userRepository.findByUserKey(userKey)
                    .map(existingUser -> {
                        String targetGender = (gender != null) ? gender : existingUser.getGender();
                        String targetAgeRange = (ageRange != null) ? ageRange : existingUser.getAgeRange();

                        // 💡 인자로 넘어온 진짜 나이(27)가 있다면 대역폭 하한선(20)보다 최우선하여 마킹됩니다.
                        Integer targetAge = (finalAge != null) ? finalAge : existingUser.getAge();

                        existingUser.updateInfo(targetGender, targetAgeRange, targetAge);
                        return existingUser;
                    })
                    .orElseGet(() -> createNewUser(userKey, nickname, gender, ageRange, finalAge));
        }

        return createNewUser("ANONYMOUS_" + UUID.randomUUID().toString().substring(0, 8), nickname, gender, ageRange, finalAge);
    }

    private User createNewUser(String userKey, String nickname, String gender, String ageRange, Integer age) {
        User newUser = new User(userKey, nickname, gender, ageRange, age);
        return userRepository.save(newUser);
    }
}