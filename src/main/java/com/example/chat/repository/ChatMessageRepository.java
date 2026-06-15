package com.example.chat.repository;

import com.example.chat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 특정 방의 대화 기록을 시간 순서대로 정렬해서 가져오기
    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(String roomId);
}