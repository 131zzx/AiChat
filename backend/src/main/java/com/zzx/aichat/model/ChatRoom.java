package com.zzx.aichat.model;

import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import lombok.Data;

import java.util.List;

@Data
public class ChatRoom {
    private String roomId;
    private List<ChatMessage> chatMessage;

}
