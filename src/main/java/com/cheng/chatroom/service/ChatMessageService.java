package com.cheng.chatroom.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cheng.chatroom.entity.ChatMessage;

import java.util.List;

public interface ChatMessageService extends IService<ChatMessage> {
    void saveChatMessage(ChatMessage message);

    IPage<ChatMessage> getHistory(Page<ChatMessage> page, String filter, String nickname);

}
