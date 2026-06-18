package com.example.chat.controller;

import com.example.chat.domain.Friendship;
import com.example.chat.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}) // 🔥 CORS 차단을 완벽하게 해제하는 무적의 방어막
public class FriendController {

    private final FriendshipRepository friendshipRepository;

    /**
     * 🤝 1. 친구 신청 보내기 API
     */
    @PostMapping("/request")
    public ResponseEntity<String> requestFriendship(@RequestBody Map<String, String> payload) {
        String requesterKey = payload.get("requesterKey");
        String receiverKey = payload.get("receiverKey");
        String requesterNickname = payload.get("requesterNickname");
        String receiverNickname = payload.get("receiverNickname");

        if (requesterKey.equals(receiverKey)) {
            return new ResponseEntity<>("자기 자신에게는 친구 신청을 할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        Optional<Friendship> alreadyExist1 = friendshipRepository.findByRequesterKeyAndReceiverKey(requesterKey, receiverKey);
        Optional<Friendship> alreadyExist2 = friendshipRepository.findByRequesterKeyAndReceiverKey(receiverKey, requesterKey);

        if (alreadyExist1.isPresent() || alreadyExist2.isPresent()) {
            return new ResponseEntity<>("이미 친구 신청을 보냈거나 이미 친구 상태입니다.", HttpStatus.CONFLICT);
        }

        Friendship friendship = new Friendship();
        friendship.setRequesterKey(requesterKey);
        friendship.setReceiverKey(receiverKey);
        friendship.setRequesterNickname(requesterNickname);
        friendship.setReceiverNickname(receiverNickname);
        friendship.setStatus("PENDING");

        friendshipRepository.save(friendship);
        return new ResponseEntity<>("친구 신청이 완료되었습니다.", HttpStatus.OK);
    }

    /**
     * 🔔 2. 나한테 온 대기 중인(PENDING) 친구 신청 목록 조회 API
     */
    @GetMapping("/pending/{userKey}")
    public ResponseEntity<List<Friendship>> getPendingRequests(@PathVariable String userKey) {
        List<Friendship> pendingList = friendshipRepository.findByReceiverKeyAndStatus(userKey, "PENDING");
        return new ResponseEntity<>(pendingList, HttpStatus.OK);
    }

    /**
     * 👥 3. 나와 연결된 수락 완료된(ACCEPTED) 진짜 친구 목록 조회 API
     */
    @GetMapping("/list/{userKey}")
    public ResponseEntity<List<Friendship>> getFriendList(@PathVariable String userKey) {
        List<Friendship> friends = friendshipRepository.findByRequesterKeyAndStatusOrReceiverKeyAndStatus(
                userKey, "ACCEPTED", userKey, "ACCEPTED"
        );
        return new ResponseEntity<>(friends, HttpStatus.OK);
    }

    /**
     * ✅ 4. 친구 신청 수락 또는 거절 API
     */
    @PostMapping("/respond")
    public ResponseEntity<String> respondToFriendship(@RequestBody Map<String, Object> payload) {
        Long friendshipId = Long.valueOf(payload.get("friendshipId").toString());
        String action = payload.get("action").toString();

        Optional<Friendship> optionalFriendship = friendshipRepository.findById(friendshipId);
        if (optionalFriendship.isEmpty()) {
            return new ResponseEntity<>("존재하지 않는 친구 요청입니다.", HttpStatus.NOT_FOUND);
        }

        Friendship friendship = optionalFriendship.get();

        if ("ACCEPT".equalsIgnoreCase(action)) {
            friendship.setStatus("ACCEPTED");
            friendshipRepository.save(friendship);
            return new ResponseEntity<>("친구 신청을 수락했습니다.", HttpStatus.OK);
        } else if ("REJECT".equalsIgnoreCase(action)) {
            friendshipRepository.delete(friendship);
            return new ResponseEntity<>("친구 신청을 거절했습니다.", HttpStatus.OK);
        }

        return new ResponseEntity<>("잘못된 요청 명령입니다.", HttpStatus.BAD_REQUEST);
    }

    /**
     * ❌ 5. 친구 삭제(절교) API
     */
    @DeleteMapping("/delete/{friendshipId}")
    public ResponseEntity<String> deleteFriend(@PathVariable Long friendshipId) {
        try {
            friendshipRepository.deleteById(friendshipId);
            return new ResponseEntity<>("SUCCESS", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("삭제 실패: 존재하지 않는 관계입니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🔄 FriendController.java 맨 아래에 추가

    /**
     * 💬 6. 친구와의 고정 대화방 정보 가져오기 (방 생성 혹은 조회)
     */
    @GetMapping("/room/{friendshipId}")
    public ResponseEntity<Map<String, String>> getFriendRoom(@PathVariable Long friendshipId) {
        // 친구 관계 ID를 기반으로 평생 고정된 방 코드를 생성합니다.
        String friendRoomId = "friend-" + friendshipId;

        // 리액트가 바로 방에 진입할 수 있도록 방 ID를 주머니에 담아 리턴합니다.
        Map<String, String> response = Map.of("roomId", friendRoomId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}