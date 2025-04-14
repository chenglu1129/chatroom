package com.cheng.chatroom.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cheng.chatroom.entity.ChatMessage;
import com.cheng.chatroom.service.ChatMessageService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket消息处理器
 * 处理所有WebSocket连接和消息
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 使用线程安全的Set保存所有活跃的WebSocket会话
    private static final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    private final ChatMessageService chatMessageService;

    // 构造器注入服务
    public ChatWebSocketHandler(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    /**
     * 处理文本消息
     * @param session 当前会话
     * @param message 收到的消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JSONObject json = JSON.parseObject(message.getPayload());
        String type = json.getString("type");

        // 构建消息实体
        ChatMessage chatMsg = new ChatMessage();
        chatMsg.setFromUser(json.getString("fromUser"));
        chatMsg.setToUser(json.getString("toUser"));
        chatMsg.setContent(json.getString("content"));
        chatMsg.setTime(new Date());
        chatMsg.setType(type);
        chatMsg.setMsgType(json.getString("msgType"));
        chatMsg.setFileUrl(json.getString("fileUrl"));
        chatMsg.setFileSize(json.getLong("fileSize"));

        try {
            // 根据消息类型处理
            switch (type) {
                case "chat":  // 群聊消息
                    broadcastChatMessage(message.getPayload());
                    break;
                case "private":  // 私聊消息
                    sendPrivateMessage(json, session);
                    break;
                case "history":  // 历史消息请求
                    handleHistoryRequest(session, json);
                    return;  // 历史消息单独处理，不需要保存
                default:
                    throw new IllegalArgumentException("未知的消息类型: " + type);
            }

            // 保存消息到数据库
            chatMessageService.saveChatMessage(chatMsg);
        } catch (Exception e) {
            // 发送错误消息给客户端
            session.sendMessage(new TextMessage(buildErrorMessage(e.getMessage())));
        }
    }

    /**
     * 处理历史消息请求
     * @param session 当前会话
     * @param payload 请求参数
     */
    private void handleHistoryRequest(WebSocketSession session, JSONObject payload) throws Exception {
        int page = payload.getIntValue("page", 1);
        int size = payload.getIntValue("size", 20);
        String filter = payload.getString("filter");
        String nickname = (String) session.getAttributes().get("nickname");

        // 创建分页对象
        Page<ChatMessage> pageParam = new Page<>(page, size);
        IPage<ChatMessage> resultPage = chatMessageService.getHistory(pageParam, filter, nickname);

        // 构建响应
        JSONObject response = new JSONObject();
        response.put("type", "history");
        response.put("messages", resultPage.getRecords());
        response.put("total", resultPage.getTotal());
        response.put("currentPage", resultPage.getCurrent());

        session.sendMessage(new TextMessage(response.toJSONString()));
    }

    /**
     * 发送私聊消息
     * @param message 消息内容
     * @param sender 发送者会话
     */
    private void sendPrivateMessage(JSONObject message, WebSocketSession sender) throws Exception {
        String fromUser = message.getString("fromUser");
        String toUser = message.getString("toUser");
        String content = message.toJSONString();

        for (WebSocketSession session : sessions) {
            String nickname = (String) session.getAttributes().get("nickname");
            // 发送给接收者或发送者自己（用于回显）
            if (session.isOpen() && (toUser.equals(nickname) || fromUser.equals(nickname))) {
                session.sendMessage(new TextMessage(content));
            }
        }
    }

    /**
     * 广播群聊消息
     * @param message 消息内容
     */
    private void broadcastChatMessage(String message) throws Exception {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    /**
     * 广播在线用户列表
     */
    private void broadcastUserList() throws Exception {
        List<String> nicknames = new ArrayList<>();
        for (WebSocketSession session : sessions) {
            String nickname = (String) session.getAttributes().get("nickname");
            if (nickname != null) {
                nicknames.add(nickname);
            }
        }

        JSONObject message = new JSONObject();
        message.put("type", "userList");
        message.put("users", nicknames);

        broadcastChatMessage(message.toJSONString());
    }

    /**
     * 广播在线人数
     */
    private void broadcastUserCount() throws Exception {
        JSONObject message = new JSONObject();
        message.put("type", "userCount");
        message.put("count", sessions.size());

        broadcastChatMessage(message.toJSONString());
    }

    /**
     * 构建错误消息
     * @param errorMsg 错误信息
     * @return JSON格式的错误消息
     */
    private String buildErrorMessage(String errorMsg) {
        JSONObject error = new JSONObject();
        error.put("type", "error");
        error.put("message", errorMsg);
        return error.toJSONString();
    }

    /**
     * 连接建立后触发
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 获取并保存昵称
        String nickname = extractNicknameFromSession(session);
        session.getAttributes().put("nickname", nickname);

        sessions.add(session);

        // 通知所有用户
        broadcastUserCount();
        broadcastUserList();
        broadcastSystemMessage(nickname + " 加入了聊天室");
    }

    /**
     * 连接关闭后触发
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String nickname = (String) session.getAttributes().get("nickname");
        sessions.remove(session);

        // 通知所有用户
        broadcastUserCount();
        broadcastUserList();
        broadcastSystemMessage(nickname + " 离开了聊天室");
    }

    /**
     * 从session中提取昵称
     */
    private String extractNicknameFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("nickname=")) {
            return query.substring("nickname=".length());
        }
        return "匿名用户";
    }

    /**
     * 广播系统消息
     */
    private void broadcastSystemMessage(String content) throws Exception {
        JSONObject message = new JSONObject();
        message.put("type", "system");
        message.put("content", content);
        message.put("time", new Date().toString());

        broadcastChatMessage(message.toJSONString());
    }

    /**
     * 处理传输错误
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
//        log.error("WebSocket传输错误: ", exception);
        session.close(CloseStatus.SERVER_ERROR);
    }
}