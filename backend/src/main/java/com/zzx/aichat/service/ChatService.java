package com.zzx.aichat.service;

import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.zzx.aichat.model.ChatRoom;

import java.util.List;

public interface ChatService {

    String doChat(Long roomId, String userPrompt);

    List<ChatRoom> getChatRooms();

}
