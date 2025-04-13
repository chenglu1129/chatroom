package com.cheng.chatroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cheng.chatroom.entity.ChatMessage;

public interface ChatMessageService extends IService<ChatMessage> {
    void saveChatMessage(ChatMessage message);
}
