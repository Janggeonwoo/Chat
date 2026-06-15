package com.example.chat.service;

import com.example.chat.repository.UserRepository;
import com.example.chat.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 로그인 또는 회원가입 처리
     * @param userKey 프론트에서 넘어온 고유 키 (없거나 처음이면 null 또는 빈 문자열)
     * @param nickname 유저가 입력한 닉네임
     */
    @Transactional
    public User loginOrRegister(String userKey, String nickname) {
        // 1. 프론트에서 보낸 고유 키가 있다면 기존 유저인지 DB에서 조회
        if (userKey != null && !userKey.trim().isEmpty()) {
            return userRepository.findByUserKey(userKey)
                    .map(existingUser -> {
                        // 기존 유저인데 닉네임을 새로 바꿨다면 업데이트
                        if (!existingUser.getNickname().equals(nickname)) {
                            existingUser.changeNickname(nickname);
                        }
                        return existingUser;
                    })
                    // 혹시 프론트에선 키가 있는데 DB에 없다면 새 유저로 가입 처리
                    .orElseGet(() -> createNewUser(nickname));
        }

        // 2. 고유 키가 없다면 아예 처음 온 유저이므로 새로 생성
        return createNewUser(nickname);
    }

    // 신규 익명 유저 생성 및 UUID 발급
    private User createNewUser(String nickname) {
        String randomKey = UUID.randomUUID().toString(); // 무작위 고유 비밀키 생성
        User newUser = new User(randomKey, nickname);
        return userRepository.save(newUser);
    }
}