package com.fun.strangerchat.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Service
public class RedisMessagePublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ChannelTopic chatTopic;

    public RedisMessagePublisher(RedisTemplate<String, String> redisTemplate,
                                 ChannelTopic chatTopic) {
        this.redisTemplate = redisTemplate;
        this.chatTopic = chatTopic;
    }

    public void publish(String partnerClientId, String message) {
        String payload = partnerClientId + "::" + message;

        System.out.println("🚀 [REDIS PUBLISH] " + payload);

        redisTemplate.convertAndSend(chatTopic.getTopic(), payload);
    }
}
