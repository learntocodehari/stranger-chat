package com.fun.strangerchat.controller;

import com.fun.strangerchat.service.MatchService;
import com.fun.strangerchat.service.RedisMessagePublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;


public class ChatController_bkp {

    private final SimpMessagingTemplate messagingTemplate;
    private final MatchService matchService;
    private final RedisMessagePublisher redisPublisher;

    public ChatController_bkp(SimpMessagingTemplate messagingTemplate,
                              MatchService matchService,
                              RedisMessagePublisher redisPublisher) {
        this.messagingTemplate = messagingTemplate;
        this.matchService = matchService;
        this.redisPublisher = redisPublisher;
    }

    @MessageMapping("/match")
    public void matchUser(@Payload String clientId, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        
        System.out.println("[MATCH] User " + clientId + " (session: " + sessionId + ") requesting match...");

        String partnerClientId = matchService.findMatch(sessionId, clientId);

        if (partnerClientId != null) {
            System.out.println("[MATCH] ✓ MATCHED: " + clientId + " <-> " + partnerClientId);
            // Send to both clients using their clientIds
            messagingTemplate.convertAndSend("/topic/match/" + partnerClientId, clientId);
            messagingTemplate.convertAndSend("/topic/match/" + clientId, partnerClientId);
        } else {
            System.out.println("[MATCH] ⏳ WAITING: " + clientId + " added to queue");
            messagingTemplate.convertAndSend("/topic/match/" + clientId, "null");
        }
    }

    @MessageMapping("/send")
    public void sendMessage(@Payload String content, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String partnerClientId = matchService.getPartnerClientId(sessionId);

        System.out.println("[SEND] Message from " + sessionId + " to " + partnerClientId + ": " + content);

        if (partnerClientId != null) {
            messagingTemplate.convertAndSend("/topic/messages/" + partnerClientId, content);
            System.out.println("[SEND] ✓ Delivered");
        } else {
            System.out.println("[SEND] ✗ ERROR: No partner found for session " + sessionId);
        }
    }

    @MessageMapping("/next")
    public void nextStranger(@Payload String clientId, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        
        System.out.println("[NEXT] User " + clientId + " (session: " + sessionId + ") moving to next stranger...");

        // Get current partner BEFORE removing
        String partnerClientId = matchService.getPartnerClientId(sessionId);

        // Notify partner that user left
        if (partnerClientId != null) {
            messagingTemplate.convertAndSend(
                    "/topic/messages/" + partnerClientId,
                    "Stranger has disconnected"
            );
            System.out.println("[NEXT] Notified partner: " + partnerClientId);
        }

        // Remove mapping
        matchService.removePartner(sessionId);
        System.out.println("[NEXT] ✓ User " + clientId + " cleared");
    }

    @MessageMapping("/typing")
    public void handleTyping(@Payload String status, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String partnerClientId = matchService.getPartnerClientId(sessionId);

        System.out.println("[TYPING] User " + sessionId + " typing status: " + status + " to " + partnerClientId);

        if (partnerClientId != null) {
            messagingTemplate.convertAndSend("/topic/typing/" + partnerClientId, status);
            System.out.println("[TYPING] ✓ Sent to partner");
        } else {
            System.out.println("[TYPING] ✗ No partner found");
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        
        // Notify partner before removing
        String partnerClientId = matchService.getPartnerClientId(sessionId);
        if (partnerClientId != null) {
            messagingTemplate.convertAndSend(
                    "/topic/messages/" + partnerClientId,
                    "Stranger has disconnected"
            );
            System.out.println("[DISCONNECT] Notified partner: " + partnerClientId);
        }
        
        matchService.removePartner(sessionId);
        System.out.println("[DISCONNECT] Session " + sessionId + " disconnected, partner removed");
    }

}