package com.fun.strangerchat.model;

import lombok.Data;

@Data
public class ChatMessage {
    private String sender;    // who sent the message
    private String receiver;  // who should receive the message
    private String content;   // the actual messageß
}
