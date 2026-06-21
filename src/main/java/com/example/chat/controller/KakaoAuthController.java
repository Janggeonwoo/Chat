package com.example.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

// 💡 본인 프로젝트의 실제 엔티티와 레포지토리 패키지 주소에 맞게 수정해 주세요!
// import com.example.chat.model.User;
// import com.example.chat.repository.UserRepository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
public class KakaoAuthController {

    // 💡 테스트 편의를 위해 임시로 주입하는 더미 레포지토리 선언입니다.
    // 실제 건우님의 @Repository 객체가 있다면 주석을 풀고 연결해 주세요!
    // private final UserRepository userRepository;

    private final String clientId = "429780ef9d280c4afe7412483ee639ff";
    private final String redirectUri = "http://localhost:8080/api/auth/kakao";

    // ─── [기능 1] 기존 카카오 인증 및 리액트 가입 분기 리다이렉트 주소 ───
    @GetMapping("/kakao")
    public ResponseEntity<Map<String, Object>> kakaoLogin(@RequestParam("code") String code) {
        String accessToken = "";
        try {
            URL url = new URL("https://kauth.kakao.com/oauth/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
            conn.setDoOutput(true);

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            sb.append("grant_type=authorization_code");
            sb.append("&client_id=").append(clientId);
            sb.append("&redirect_uri=").append(redirectUri);
            sb.append("&code=").append(code);

            bw.write(sb.toString());
            bw.flush();
            bw.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                String line = "";
                StringBuilder result = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }
                br.close();

                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> tokenMap = mapper.readValue(result.toString(), Map.class);
                accessToken = (String) tokenMap.get("access_token");
            } else {
                throw new RuntimeException("토큰 발급 오류 응답 코드: " + responseCode);
            }

            URL userUrl = new URL("https://kapi.kakao.com/v2/user/me");
            HttpURLConnection userConn = (HttpURLConnection) userUrl.openConnection();
            userConn.setRequestMethod("POST");
            userConn.setRequestProperty("Authorization", "Bearer " + accessToken);
            userConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            int userResponseCode = userConn.getResponseCode();
            if (userResponseCode == 200) {
                BufferedReader userBr = new BufferedReader(new InputStreamReader(userConn.getInputStream(), "UTF-8"));
                String userLine = "";
                StringBuilder userResult = new StringBuilder();
                while ((userLine = userBr.readLine()) != null) {
                    userResult.append(userLine);
                }
                userBr.close();

                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> userInfoMap = mapper.readValue(userResult.toString(), Map.class);

                Object idObj = userInfoMap.get("id");
                Long kakaoId = (idObj instanceof Integer) ? ((Integer) idObj).longValue() : (Long) idObj;

                Map<String, Object> kakaoAccount = (Map<String, Object>) userInfoMap.get("kakao_account");
                String kakaoNickname = "익명사용자";

                if (kakaoAccount != null) {
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                    if (profile != null && profile.get("nickname") != null) {
                        kakaoNickname = (String) profile.get("nickname");
                    }
                }

                System.out.println("🎉 카카오 본인 확인 통과! ID: " + kakaoId);

                // 리액트 메인 대문으로 가입 필수 사인을 실어서 리다이렉트
                String redirectUrl = "http://localhost:5173"
                        + "?status=SIGNUP_REQUIRED"
                        + "&kakaoId=" + kakaoId
                        + "&defaultNickname=" + java.net.URLEncoder.encode(kakaoNickname, "UTF-8");

                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, redirectUrl)
                        .build();

            } else {
                throw new RuntimeException("유저 정보 수신 오류 응답 코드: " + userResponseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─── [기능 2] 🔥 신규 추가: 계정 정보 생성 단계 최종 회원가입 및 DB 적재 ───
    // ⭕ 변경할 코드
    @PostMapping("/signup/final")
    public ResponseEntity<Map<String, Object>> handleFinalSignup(@RequestBody Map<String, Object> requestBody) {
        try {
            String kakaoId = String.valueOf(requestBody.get("kakaoId"));
            String userId = (String) requestBody.get("userId");
            String userPw = (String) requestBody.get("userPw");
            String nickname = (String) requestBody.get("nickname");

            System.out.println("📝 회원가입 요청 접수 완료 -> ID: " + userId + ", 별명: " + nickname);

            // 💡 [건우님의 숙제]: 아래 유저 엔티티 생성 주석들을 풀고 실제 DB 저장을 활성화해 주세요!
            /*
            User user = new User();
            user.setUserKey(UUID.randomUUID().toString()); // 고유 랜덤 난수키 발급
            user.setKakaoId(kakaoId);
            user.setUserId(userId);    // 👈 추가한 아이디 컬럼 매핑
            user.setUserPw(userPw);    // 👈 추가한 비밀번호 컬럼 매핑
            user.setNickname(nickname);

            userRepository.save(user); // 실제 데이터베이스 하드 디스크에 꽂아 넣기!
            */

            // 리액트 회원가입 성공 얼럿 제어용 가짜 더미 응답 데이터 리턴
            Map<String, Object> response = new HashMap<>();
            response.put("userKey", UUID.randomUUID().toString()); // 임시 키 반환
            response.put("nickname", nickname);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─── [기능 3] 🔥 신규 추가: 첫 화면 로그인창 전용 일반 로그인 인증 주소 ───
    @PostMapping("/login/normal")
    public ResponseEntity<Map<String, Object>> handleNormalLogin(@RequestBody Map<String, Object> requestBody) {
        String userId = (String) requestBody.get("userId");
        String userPw = (String) requestBody.get("userPw");

        System.out.println("🔑 일반 로그인 요청 검증 중 -> ID: " + userId);

        // 💡 [건우님의 숙제]: 실제 DB 연동 로그인 매칭 검증부입니다.
        /*
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 유저입니다."));

        if (!user.getUserPw().equals(userPw)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED); // 비번 다르면 401 퇴출
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userKey", user.getUserKey());
        response.put("nickname", user.getNickname());
        return ResponseEntity.ok(response);
        */

        // 임시 테스트용 목업 무사통과 더미 데이터 반환
        Map<String, Object> response = new HashMap<>();
        response.put("userKey", "normal-user-test-key-1234");
        response.put("nickname", "일반테스터");
        return ResponseEntity.ok(response);
    }
}