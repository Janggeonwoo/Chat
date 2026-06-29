package com.example.chat.controller;

import com.example.chat.domain.User;
import com.example.chat.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
public class KakaoAuthController {

    private final UserService userService;

    private final String clientId = "429780ef9d280c4afe7412483ee639ff";
    private final String redirectUri = "http://localhost:8080/api/auth/kakao";

    /**
     * ─── 💛 [코어 로직] 카카오 인가 코드 수신 및 1초 패스 자동 가입/로그인 ───
     */
    @GetMapping("/kakao")
    public ResponseEntity<Void> kakaoLogin(@RequestParam("code") String code) {
        String accessToken = "";
        try {
            // 1. 카카오 토큰 발급 요청
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
                while ((line = br.readLine()) != null) { result.append(line); }
                br.close();

                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> tokenMap = mapper.readValue(result.toString(), Map.class);
                accessToken = (String) tokenMap.get("access_token");
            } else {
                throw new RuntimeException("카카오 토큰 발급 실패. 응답 코드: " + responseCode);
            }

            // 2. 발급받은 토큰으로 카카오 사용자 프로필 정보 수신
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
                while ((userLine = userBr.readLine()) != null) { userResult.append(userLine); }
                userBr.close();

                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> userInfoMap = mapper.readValue(userResult.toString(), Map.class);

                // 고유 카카오 ID 추출
                Object idObj = userInfoMap.get("id");
                Long kakaoId = (idObj instanceof Integer) ? ((Integer) idObj).longValue() : (Long) idObj;

                String kakaoNickname = "낯선 상대";

                // 💾 3. 서비스단에 조회/저장 요청 (성별, 나이는 모르므로 null 대입)
                User user = userService.loginOrRegister("KAKAO_" + kakaoId, kakaoNickname, null, null, null);

                // 💡 [🚨 핵심 점검 및 수정존]
                // 가입되거나 가져온 유저 엔티티의 gender가 완전히 null이거나 비어있으면 PROFILE_REQUIRED를 강제 주입!
                String statusFlag;
                if (user.getGender() == null || user.getGender().trim().isEmpty()) {
                    statusFlag = "PROFILE_REQUIRED";
                } else {
                    statusFlag = "LOGIN_SUCCESS";
                }

                System.out.println("🚨 [컨트롤러 판단 로그] -> 유저키: " + user.getUserKey() + " | DB성별: " + user.getGender() + " | 최종 전송 신호: " + statusFlag);

                // ➡️ 4. 프론트엔드로 신호를 심어서 리다이렉트 처리
                String redirectUrl = "http://localhost:5173"
                        + "?status=" + statusFlag
                        + "&userKey=" + user.getUserKey()
                        + "&nickname=" + java.net.URLEncoder.encode(user.getNickname(), "UTF-8");

                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, redirectUrl)
                        .build();

            } else {
                throw new RuntimeException("카카오 유저 정보 수신 실패. 응답 코드: " + userResponseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}