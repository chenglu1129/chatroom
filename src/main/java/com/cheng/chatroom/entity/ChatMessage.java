package com.cheng.chatroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 聊天消息实体类
 * 确保所有字段与数据库表列名匹配
 */
@Data
@TableName("chat_message") // 明确指定表名
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("from_user") // 明确指定列名
    private String fromUser;

    @TableField("to_user")
    private String toUser;

    @TableField("content")
    private String content;

    @TableField("message_type") // 建议使用下划线命名
    private String msgType;

    @TableField("file_url")
    private String fileUrl;

    @TableField("file_size")
    private Long fileSize;

    @TableField("create_time") // 明确时间字段
    private Date time;

    @TableField("type")
    private String type; // chat/private

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;

    @Version
    @TableField("version")
    private Integer version;
}