package com.cheng.chatroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cheng.chatroom.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
