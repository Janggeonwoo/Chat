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
    // 어떤 세션ID가 어떤 방(RoomId)에 들어가 있는지 추적하는 지도
    private static final Map<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    // 어떤 세션ID의 닉네임이 무엇인지 추적하는 지도
    private static final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    // 💡 어떤 세션ID가 어떤 고유 유저키(userKey)를 가졌는지 추적하는 지도
    private static final Map<String, String> sessionKeyMap = new ConcurrentHashMap<>();

    /**
     * 📩 1. 실시간 대화 전달 + DB 저장 (TEXT, IMAGE 타입 완벽 방어)
     */
    @MessageMapping("/message/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, ChatMessage message) {
        message.setRoomId(roomId);

        // 프론트가 보낸 타입(TEXT 또는 IMAGE)이 없으면 기본값 TEXT 세팅
        if (message.getType() == null) {
            message.setType("TEXT");
        }

        chatMessageRepository.save(message); // 이제 type 컬럼에 IMAGE나 TEXT가 정확히 박힙니다!
        messagingTemplate.convertAndSend("/sub/chatroom/" + roomId, message);
    }

    /**
     * ⚡ 2. 랜덤 매칭 신청 및 정보 등록 (상대방 고유 식별 정보 동시 교환 버전으로 단일화)
     */
    @MessageMapping("/match/join")
    public void joinMatch(SimpMessageHeaderAccessor headerAccessor, Map<String, String> payload) {
        String sessionId = headerAccessor.getSessionId();

        // 프론트엔드가 보낸 닉네임과 유저키 가로채기
        String myNickname = headerAccessor.getFirstNativeHeader("nickname");
        String myUserKey = headerAccessor.getFirstNativeHeader("userKey");

        if (myNickname == null) myNickname = "유저";
        if (myUserKey == null) myUserKey = "unknown_key";

        sessionUserMap.put(sessionId, myNickname);
        sessionKeyMap.put(sessionId, myUserKey); // 지도에 내 기기 고유 키값 저장

        System.out.println("매칭 신청! 세션 ID: " + sessionId + " (닉네임: " + myNickname + " / 키: " + myUserKey + ")");

        if (waitingQueue.isEmpty()) {
            waitingQueue.add(sessionId);
        } else {
            String partnerId = waitingQueue.poll(); // 먼저 기다리던 선공자 꺼내기
            String secretRoomId = "room-" + UUID.randomUUID().toString().substring(0, 8);

            // 두 유저가 같은 방에 들어갔다고 매핑 지도에 기록
            sessionRoomMap.put(partnerId, secretRoomId);
            sessionRoomMap.put(sessionId, secretRoomId);

            // 상대방(선공자)의 정보 추출
            String partnerNickname = sessionUserMap.get(partnerId);
            String partnerUserKey = sessionKeyMap.get(partnerId);

            // 🎁 1번 유저(선공자)에게 배달할 주머니 (상대방인 내 정보 탑재)
            Map<String, String> responseToPartner = Map.of(
                    "roomId", secretRoomId,
                    "opponentKey", myUserKey,
                    "opponentNickname", myNickname
            );

            // 🎁 2번 유저(나)에게 배달할 주머니 (상대방인 선공자 정보 탑재)
            Map<String, String> responseToMe = Map.of(
                    "roomId", secretRoomId,
                    "opponentKey", partnerUserKey,
                    "opponentNickname", partnerNickname
            );

            // 암묵적 캐스팅 모호성 에러 해결용 (String), (Object) 보완 탑재
            messagingTemplate.convertAndSend((String) "/queue/match/" + partnerId, (Object) responseToPartner);
            messagingTemplate.convertAndSend((String) "/queue/match/" + sessionId, (Object) responseToMe);
        }
    }

    /**
     * 🚪 3. [수동 퇴장] 사용자가 버튼을 눌러 나갔을 때
     */
    @MessageMapping("/match/leave")
    public void leaveMatch(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        String roomId = sessionRoomMap.remove(sessionId);
        String userNickname = sessionUserMap.remove(sessionId);
        sessionKeyMap.remove(sessionId); // 💡 리셋 시 유저 키 찌꺼기 청소

        if (roomId != null && userNickname != null) {
            System.out.println("🚪 [수동 퇴장] " + userNickname + "님이 버튼을 눌러 나갔습니다. 방: " + roomId);

            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setRoomId(roomId);
            systemMessage.setSender("시스템");
            systemMessage.setContent(userNickname + "님이 대화방을 나갔습니다.");
            systemMessage.setType("TEXT");

            messagingTemplate.convertAndSend("/sub/chatroom/" + roomId, systemMessage);
        }
    }

    /**
     * 🔌 4. [자동 퇴장] 브라우저 종료, 새로고침 등 웹소켓 끊김 감지 시
     */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        waitingQueue.remove(sessionId); // 대기열 자동 탈출

        String roomId = sessionRoomMap.remove(sessionId);
        String userNickname = sessionUserMap.remove(sessionId);
        sessionKeyMap.remove(sessionId); // 💡 세션 만료 시 유저 키 찌꺼기 청소

        if (roomId != null && userNickname != null) {
            System.out.println("🚨 [연결 끊김] " + userNickname + "님이 방을 나갔습니다. 방 코드: " + roomId);

            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setRoomId(roomId);
            systemMessage.setSender("시스템");
            systemMessage.setContent(userNickname + "님이 대화방을 나갔습니다.");
            systemMessage.setType("TEXT");

            messagingTemplate.convertAndSend("/sub/chatroom/" + roomId, systemMessage);
        }
    }

    /**
     * ⌨️ 5. 상대방 입력 상태 실시간 중계 라우터
     */
    @MessageMapping("/message/{roomId}/typing")
    public void sendTypingStatus(
            @DestinationVariable String roomId,
            Map<String, Object> payload
    ) {
        // 프론트엔드가 보낸 { sender: "닉네임", isTyping: true/false } 구조를 상대에게 중계
        messagingTemplate.convertAndSend((String) "/sub/chatroom/" + roomId + "/typing", (Object) payload);
    }

    /**
     * 📜 6. 과거 대화 내역 싹 긁어오기 API
     */
    @GetMapping("/api/chat/room/{roomId}/messages")
    @ResponseBody
    public List<ChatMessage> getRoomMessages(@PathVariable String roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
    }

    // 🔄 ChatController.java 내부에 추가

    @MessageMapping("/match/cancel")
    public void cancelMatch(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        // 대기열(큐)에서 내 세션ID를 찾아서 제거합니다.
        boolean removed = waitingQueue.remove(sessionId);

        if (removed) {
            System.out.println("❌ [매칭 취소] 세션 ID: " + sessionId + " 님이 매칭을 취소하고 로비로 복귀했습니다.");
        }
    }
}