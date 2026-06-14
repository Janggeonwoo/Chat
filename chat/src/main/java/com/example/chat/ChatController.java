package com.example.chat;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @MessageMapping("/message") // /pub/message로 들어오는 톡을 낚아채서
    @SendTo("/sub/chatroom")   // /sub/chatroom 구독자들에게 실시간 토스!
    public ChatMessage broadcastMessage(ChatMessage message) {
        return message;
    }
}