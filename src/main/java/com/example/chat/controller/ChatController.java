package com.example.chat.controller;

import com.example.chat.domain.ChatMessage;
import com.example.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Controller
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    // 대기열 및 세션 추적용 안전한 저장소 세트
    private static final Queue<String> waitingQueue = new ConcurrentLinkedQueue<>();
    // 💡 어떤 세션ID가 어떤 방(RoomId)에 들어가 있는지 추적하는 지도
    private static final Map<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    // 💡 어떤 세션ID의 닉네임이 무엇인지 추적하는 지도
    private static final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    // 📩 실시간 대화 전달 + DB 저장
    @MessageMapping("/message/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, ChatMessage message) {
        message.setRoomId(roomId);
        chatMessageRepository.save(message); // DB 저장
        messagingTemplate.convertAndSend("/sub/chatroom/" + roomId, message);
    }

    // ⚡ 랜덤 매칭 신청 및 정보 등록
    @MessageMapping("/match/join")
    public void joinMatch(SimpMessageHeaderAccessor headerAccessor, Map<String, String> payload) {
        String sessionId = headerAccessor.getSessionId();
        // 프론트엔드가 보낸 내 닉네임을 가로채서 추적 지도에 등록합니다.
        String myNickname = headerAccessor.getFirstNativeHeader("nickname");
        if (myNickname == null) myNickname = "익명유저";

        sessionUserMap.put(sessionId, myNickname);
        System.out.println("매칭 신청! 세션 ID: " + sessionId + " (닉네임: " + myNickname + ")");

        if (waitingQueue.isEmpty()) {
            waitingQueue.add(sessionId);
        } else {
            String partnerId = waitingQueue.poll();
            String secretRoomId = "room-" + UUID.randomUUID().toString().substring(0, 8);

            // ⭐️ 중요: 두 유저가 같은 방에 들어갔다고 추적 지도에 기록!
            sessionRoomMap.put(partnerId, secretRoomId);
            sessionRoomMap.put(sessionId, secretRoomId);

            Map<String, String> responseMap = Map.of("roomId", secretRoomId);
            messagingTemplate.convertAndSend("/queue/match/" + partnerId, responseMap);
            messagingTemplate.convertAndSend("/queue/match/" + sessionId, responseMap);
        }
    }

    /**
     * 🔌 3. 한 명이 도망갔을(나갔을) 때 상대방에게 알림 쏘기
     */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        waitingQueue.remove(sessionId); // 대기열 탈출

        // 나간 사람이 있던 방 ID와 닉네임 꺼내기
        String roomId = sessionRoomMap.remove(sessionId);
        String userNickname = sessionUserMap.remove(sessionId);

        // 방에 묶여있던 대화방인 경우에만 상대방에게 알림 전송
        if (roomId != null && userNickname != null) {
            System.out.println("🚨 [" + userNickname + "] 님이 방을 나갔습니다. 방 코드: " + roomId);

            // 상대방 화면에 띄워줄 "퇴장 안내 메시지" 규격 생성
            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setRoomId(roomId);
            systemMessage.setSender("시스템");
            systemMessage.setContent(userNickname + "님이 대화방을 나갔습니다.");

            // ⭐️ 퇴장 메시지도 원하신다면 DB에 저장 가능! (여기선 실시간 전달만 진행)
            messagingTemplate.convertAndSend("/sub/chatroom/" + roomId, systemMessage);
        }
    }

    @GetMapping("/api/chat/room/{roomId}/messages")
    @ResponseBody
    public List<ChatMessage> getRoomMessages(@PathVariable String roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
    }

    // 🔄 ChatController.java 내부에 추가할 메서드

    @MessageMapping("/match/leave")
    public void leaveMatch(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        // handleDisconnect 메서드와 동일한 퇴장 로직 수행
        String roomId = sessionRoomMap.remove(sessionId);
        String userNickname = sessionUserMap.remove(sessionId);

        if (roomId != null && userNickname != null) {
            System.out.println("🚪 [수동 퇴장] " + userNickname + "님이 버튼을 눌러 나갔습니다. 방: " + roomId);

            // 방에 남은 사람에게 퇴장 알림 쏘기
            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setRoomId(roomId);
            systemMessage.setSender("시스템");
            systemMessage.setContent(userNickname + "님이 대화방을 나갔습니다.");

            messagingTemplate.convertAndSend("/sub/chatroom/" + roomId, systemMessage);
        }
    }
}