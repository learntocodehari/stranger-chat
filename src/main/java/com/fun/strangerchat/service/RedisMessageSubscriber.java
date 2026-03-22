package com.fun.strangerchat.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisMessageSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    public RedisMessageSubscriber(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void receiveMessage(String message) {

        // format: partnerId::TYPE::content
        String[] parts = message.split("::", 3);
        if (parts.length < 2) {
            System.out.println("[REDIS SUBSCRIBE] Skipping malformed payload: " + message);
            return;
        }

        String partnerClientId = parts[0];
        String type = parts[1];
        String content = parts.length > 2 ? parts[2] : "";

        System.out.println("[REDIS SUBSCRIBE] " + partnerClientId + "::" + type + "::" + content);

        switch (type) {

            case "MSG":
                messagingTemplate.convertAndSend(
                        "/topic/messages/" + partnerClientId,
                        content
                );
                break;

            case "TYPING":
                messagingTemplate.convertAndSend(
                        "/topic/typing/" + partnerClientId,
                        content
                );
                break;

            case "SYSTEM":
                messagingTemplate.convertAndSend(
                        "/topic/messages/" + partnerClientId,
                        content
                );
                break;
        }
    }
}
