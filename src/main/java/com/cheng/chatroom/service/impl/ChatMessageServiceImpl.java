package com.cheng.chatroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cheng.chatroom.entity.ChatMessage;
import com.cheng.chatroom.mapper.ChatMessageMapper;
import com.cheng.chatroom.service.ChatMessageService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {
    private final ChatMessageMapper chatMessageMapper;

    public ChatMessageServiceImpl(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public void saveChatMessage(ChatMessage message) {
        this.save(message); // 保存消息
    }

    @Override
    public IPage<ChatMessage> getHistory(Page<ChatMessage> page, String filter, String nickname) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();

        if (Objects.equals(filter, "self-private")) {
            // 私聊，且与自己相关
            wrapper.eq("type", "private")
                    .and(w -> w.eq("from_user", nickname).or().eq("to_user", nickname));
        } else if (Objects.equals(filter, "group")) {
            // 群聊
            wrapper.eq("type", "chat");
        }
        wrapper.orderByDesc("create_time");

        return chatMessageMapper.selectPage(page, wrapper);
    }
}
