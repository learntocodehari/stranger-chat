package com.fun.strangerchat.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MatchService_bkp {

    // sessionId -> clientId mapping
    private final Map<String, String> sessionToClientId = new ConcurrentHashMap<>();
    // clientId -> sessionId mapping
    private final Map<String, String> clientIdToSession = new ConcurrentHashMap<>();
    // Queue of waiting clientIds
    private final Queue<String> waitingQueue = new ConcurrentLinkedQueue<>();
    // clientId -> partner clientId mapping
    private final Map<String, String> partners = new ConcurrentHashMap<>();

    public synchronized String findMatch(String sessionId, String clientId) {
        // Store session <-> clientId mapping
        sessionToClientId.put(sessionId, clientId);
        clientIdToSession.put(clientId, sessionId);

        // If already matched, return existing partner (don't rematch)
        if (partners.containsKey(clientId)) {
            System.out.println("[MATCH] Already matched: " + clientId + " -> " + partners.get(clientId));
            return partners.get(clientId);
        }

        String partnerClientId = waitingQueue.poll();

        if (partnerClientId == null) {
            waitingQueue.add(clientId);
            System.out.println("[MATCH] ⏳ WAITING: " + clientId + " added to queue");
            return null;
        }

        // Store mapping BOTH ways (using clientIds, not sessionIds)
        partners.put(clientId, partnerClientId);
        partners.put(partnerClientId, clientId);

        return partnerClientId;
    }

    public String getPartnerClientId(String sessionId) {
        String clientId = sessionToClientId.get(sessionId);
        if (clientId == null) return null;
        return partners.get(clientId);
    }

    public void removePartner(String sessionId) {
        String clientId = sessionToClientId.get(sessionId);
        if (clientId != null) {
            String partnerClientId = partners.remove(clientId);
            if (partnerClientId != null) {
                partners.remove(partnerClientId);
            }
            sessionToClientId.remove(sessionId);
            clientIdToSession.remove(clientId);
        }
    }
}