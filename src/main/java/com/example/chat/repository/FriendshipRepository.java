package com.example.chat.repository;

import com.example.chat.domain.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // 1. 나한테 온 친구 신청 목록 조회 (상태가 PENDING이고 내가 receiverKey인 경우)
    List<Friendship> findByReceiverKeyAndStatus(String receiverKey, String status);

    // 2. 내가 보냈거나(requester) 혹은 받았는데(receiver) 이미 친구 완료(ACCEPTED)된 전체 목록 조회
    List<Friendship> findByRequesterKeyAndStatusOrReceiverKeyAndStatus(
            String requesterKey, String status1, String receiverKey, String status2
    );

    // 3. 이미 두 사람 사이에 맺어진 친구 관계 데이터가 있는지 단건 확인 (중복 신청 방어용)
    Optional<Friendship> findByRequesterKeyAndReceiverKey(String requesterKey, String receiverKey);
}