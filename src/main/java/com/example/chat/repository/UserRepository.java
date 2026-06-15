package com.example.chat.repository;

import com.example.chat.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 프론트에서 보낸 고유 비밀키(UUID)로 유저가 이미 존재하는지 찾는 메서드
    Optional<User> findByUserKey(String userKey);
}