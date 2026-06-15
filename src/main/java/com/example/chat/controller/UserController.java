package com.example.chat.controller;

import com.example.chat.domain.User;
import com.example.chat.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // 프론트엔드 포트가 달라도 통신이 가능하도록 CORS 허용
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String userKey = request.get("userKey");
        String nickname = request.get("nickname");

        // 서비스 로직 실행
        User user = userService.loginOrRegister(userKey, nickname);

        // 프론트엔드에게 유저의 닉네임과 '비밀 고유키(userKey)'를 돌려줌
        // 기존의 response.add(...) 코드를 put으로 수정합니다.
        Map<String, Object> response = new HashMap<>();
        response.put("userKey", user.getUserKey());
        response.put("nickname", user.getNickname());

        return ResponseEntity.ok(response);
    }
}