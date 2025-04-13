package com.cheng.chatroom.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cheng.chatroom.entity.ChatMessage;
import com.cheng.chatroom.mapper.ChatMessageMapper;
import com.cheng.chatroom.service.ChatMessageService;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {
}
