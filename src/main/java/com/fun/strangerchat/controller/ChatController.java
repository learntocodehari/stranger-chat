package com.fun.strangerchat.controller;

import com.fun.strangerchat.service.MatchService;
import com.fun.strangerchat.service.RedisMessagePublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MatchService matchService;
    private final RedisMessagePublisher redisPublisher;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          MatchService matchService,
                          RedisMessagePublisher redisPublisher) {
        this.messagingTemplate = messagingTemplate;
        this.matchService = matchService;
        this.redisPublisher = redisPublisher;
    }

    @MessageMapping("/match")
    public void matchUser(@Payload String clientId, SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        System.out.println("[MATCH] User " + clientId + " requesting match...");

        String partnerClientId = matchService.findMatch(sessionId, clientId);

        if (partnerClientId != null) {

            System.out.println("[MATCH] ✓ MATCHED: " + clientId + " <-> " + partnerClientId);

            // Notify both users locally (safe, since both are connected via Redis-backed flow)
            messagingTemplate.convertAndSend("/topic/match/" + partnerClientId, clientId);
            messagingTemplate.convertAndSend("/topic/match/" + clientId, partnerClientId);

        } else {
            messagingTemplate.convertAndSend("/topic/match/" + clientId, "null");
        }
    }

    @MessageMapping("/send")
    public void sendMessage(@Payload String content, SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        String partnerClientId = matchService.getPartnerClientId(sessionId);

        System.out.println("[SEND] " + sessionId + " -> " + partnerClientId + " : " + content);

        if (partnerClientId != null) {

            // 🔥 Send via Redis (multi-server safe)
            redisPublisher.publish(partnerClientId, "MSG::" + content);

            System.out.println("[SEND] ✓ Published to Redis");

        } else {
            System.out.println("[SEND] ✗ No partner");
        }
    }

    @MessageMapping("/next")
    public void nextStranger(@Payload String clientId, SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        System.out.println("[NEXT] " + clientId + " moving to next");

        String partnerClientId = matchService.getPartnerClientId(sessionId);

        if (partnerClientId != null) {

            // 🔥 Notify via Redis
            redisPublisher.publish(partnerClientId, "SYSTEM::Stranger has disconnected");

            System.out.println("[NEXT] ✓ Notified partner");
        }

        matchService.removePartner(sessionId);
    }

    @MessageMapping("/typing")
    public void handleTyping(@Payload String status, SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        String partnerClientId = matchService.getPartnerClientId(sessionId);

        if (partnerClientId != null) {

            // 🔥 Typing via Redis
            redisPublisher.publish(partnerClientId, "TYPING::" + status);

        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {

        String sessionId = event.getSessionId();
        String partnerClientId = matchService.getPartnerClientId(sessionId);

        if (partnerClientId != null) {

            // 🔥 Notify via Redis
            redisPublisher.publish(partnerClientId, "SYSTEM::Stranger disconnected");

            System.out.println("[DISCONNECT] Notified partner");
        }

        matchService.removePartner(sessionId);
    }
}