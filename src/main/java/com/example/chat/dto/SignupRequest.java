package com.example.chat.dto; // 혹은 com.example.chat.dto

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {
    private String kakaoId;
    private String userId;
    private String userPw;
    private String nickname;
}