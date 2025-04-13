package com.cheng.chatroom.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cheng.chatroom.entity.ChatMessage;
import com.cheng.chatroom.service.ChatMessageService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    private final ChatMessageService chatMessageService;
    public ChatWebSocketHandler(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JSONObject json = JSON.parseObject(message.getPayload());
        String type = json.getString("type");

        ChatMessage chatMsg = new ChatMessage();
        chatMsg.setFromUser(json.getString("from"));
        chatMsg.setToUser(json.getString("to"));
        chatMsg.setContent(json.getString("content"));
        chatMsg.setTime(json.getString("time"));
        chatMsg.setType(json.getString("type"));

        if ("chat".equals(type)) {
            // 群发消息
            broadcastChatMessage(message.getPayload());
        } else if ("private".equals(type)) {
            String to = json.getString("to");
            sendPrivateMessage(json, to, session);
        }
        chatMessageService.saveChatMessage(chatMsg); // 保存
    }
    //私信
    private void sendPrivateMessage(JSONObject json, String to, WebSocketSession sender) throws Exception {
        String messageStr = JSON.toJSONString(json);
        for (WebSocketSession s : sessions) {
            String nick = (String) s.getAttributes().get("nickname");
            if (nick != null && (nick.equals(to) || s == sender)) {
                s.sendMessage(new TextMessage(messageStr));
            }
        }
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);

        // 获取昵称参数
        String nickname = getNickname(session);
        if (nickname != null) {
            session.getAttributes().put("nickname", nickname);
        }

        sendHistoryToUser(session);
        broadcastUserCount();
        broadcastUserList(); // 广播在线用户列表
        broadcastSystemMessage("有新用户加入");
    }

    private String getNickname(WebSocketSession session) {
        String query = session.getUri().getQuery(); // nickname=xxx
        if (query != null && query.startsWith("nickname=")) {
            return query.substring("nickname=".length());
        }
        return "匿名用户";
    }

    private void broadcastUserList() throws Exception {
        List<String> nicknames = new ArrayList<>();
        for (WebSocketSession s : sessions) {
            Object nick = s.getAttributes().get("nickname");
            if (nick != null) {
                nicknames.add(nick.toString());
            }
        }

        // 构造 JSON 消息
        JSONObject messageJson = new JSONObject();
        messageJson.put("type", "userList");
        messageJson.put("users", JSONArray.from(nicknames));

        String message = messageJson.toJSONString();

        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(message));
            }
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        broadcastUserCount();
        broadcastUserList(); // 更新用户列表
        broadcastSystemMessage("有用户离开");
    }


    // 发送历史记录给新用户
    private void sendHistoryToUser(WebSocketSession session) throws Exception {
        List<ChatMessage> messages = chatMessageService.list();
        for (ChatMessage msg : messages) {
//            String content = "[" + msg.getCreateTime() + "] " + msg.getNickname() + "：" + msg.getMessage();
            String json = JSONObject.toJSONString(msg);
            session.sendMessage(new TextMessage(json));
        }
    }


    // 广播在线人数
    private void broadcastUserCount() throws Exception {
        String message = "{\"type\":\"userCount\", \"content\":\"" + sessions.size() + "\"}";
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(message));
            }
        }
    }

    // 广播系统消息
    private void broadcastSystemMessage(String content) throws Exception {
        String message = "{\"type\":\"system\", \"content\":\"【系统消息】：" + content + "，当前在线人数：" + sessions.size() + "\"}";
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(message));
            }
        }
    }

    // 广播聊天消息
    private void broadcastChatMessage(String message) throws Exception {
//        String json = "{\"type\":\"chat\", \"content\":\"" + message + "\"}";
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(message));
            }
        }
    }
}
