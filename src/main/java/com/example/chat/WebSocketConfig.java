package com.example.chat;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOrigins("http://localhost:5173")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /queue(1:1 귓속말)와 /sub(단체방) 브로커 활성화
        registry.enableSimpleBroker("/sub", "/queue");

        // 리액트가 톡을 보낼 때 쓰는 주소 접두사
        registry.setApplicationDestinationPrefixes("/pub");

        // 🔥 핵심: 스프링이 1:1 유저에게 귓속말을 보낼 때 사용할 대상 접두사를 명시합니다.
        registry.setUserDestinationPrefix("/user");
    }
}