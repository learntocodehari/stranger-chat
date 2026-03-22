package com.fun.strangerchat.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchService {

    private final RedisTemplate<String, String> redisTemplate;

    // sessionId -> clientId (keep local)
    private final Map<String, String> sessionToClientId = new ConcurrentHashMap<>();

    public MatchService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String WAITING_QUEUE = "waiting_users";
    private static final String PARTNERS = "active_chats";

    public synchronized String findMatch(String sessionId, String clientId) {

        sessionToClientId.put(sessionId, clientId);

        // check if already matched
        String existingPartner = (String) redisTemplate.opsForHash().get(PARTNERS, clientId);
        if (existingPartner != null) {
            return existingPartner;
        }

        // get someone from queue
        String partnerClientId = redisTemplate.opsForList().leftPop(WAITING_QUEUE);

        if (partnerClientId == null || partnerClientId.equals(clientId)) {
            redisTemplate.opsForList().rightPush(WAITING_QUEUE, clientId);
            return null;
        }

        // store both sides
        redisTemplate.opsForHash().put(PARTNERS, clientId, partnerClientId);
        redisTemplate.opsForHash().put(PARTNERS, partnerClientId, clientId);

        return partnerClientId;
    }

    public String getPartnerClientId(String sessionId) {
        String clientId = sessionToClientId.get(sessionId);
        if (clientId == null) return null;

        return (String) redisTemplate.opsForHash().get(PARTNERS, clientId);
    }

    public void removePartner(String sessionId) {

        String clientId = sessionToClientId.get(sessionId);
        if (clientId == null) return;

        String partnerClientId = (String) redisTemplate.opsForHash().get(PARTNERS, clientId);

        if (partnerClientId != null) {
            redisTemplate.opsForHash().delete(PARTNERS, clientId);
            redisTemplate.opsForHash().delete(PARTNERS, partnerClientId);
        }

        // remove from waiting queue also
        redisTemplate.opsForList().remove(WAITING_QUEUE, 1, clientId);

        sessionToClientId.remove(sessionId);
    }
}