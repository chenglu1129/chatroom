package com.cheng.chatroom.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId
    private Long id;
    private String type;
    private String nickname;
    private String message;
    private String createTime;
}
