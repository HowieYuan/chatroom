package com.howie.service;

import com.alibaba.fastjson.JSONObject;
import com.howie.model.User;
import com.howie.model.WebSocketMessage;
import com.howie.util.DateUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.howie.constant.MessageCodeConstant.*;

/**
 * Created with IntelliJ IDEA
 *
 * @Author yuanhaoyue swithaoy@gmail.com
 * @Description
 * @Date 2018-05-12
 * @Time 20:57
 */
public class MessageService {
    public String getSystemMessageJSONString(String chat, int systemMessgaeCode) {
        WebSocketMessage webSocketMessage = new WebSocketMessage();
        webSocketMessage.setCode(SYSTEM_MESSAGE_CODE);
        webSocketMessage.setMessage(chat);
        webSocketMessage.setTime(DateUtils.date2String(new Date()));
        Map<String, Object> map = new HashMap<>();
        map.put("systemMessageCode", systemMessgaeCode);
        webSocketMessage.setBody(map);
        return JSONObject.toJSONString(webSocketMessage);
    }

    public String getSystemMessageJSONString(String chat, int systemMessgaeCode, Object o) {
        WebSocketMessage webSocketMessage = new WebSocketMessage();
        webSocketMessage.setCode(SYSTEM_MESSAGE_CODE);
        webSocketMessage.setMessage(chat);
        webSocketMessage.setTime(DateUtils.date2String(new Date()));
        Map<String, Object> map = new HashMap<>();
        map.put("systemMessageCode", systemMessgaeCode);
        map.put("object", o);
        webSocketMessage.setBody(map);
        return JSONObject.toJSONString(webSocketMessage);
    }

    public String getGroupChatMessageJSONString(User user, String chat) {
        WebSocketMessage webSocketMessage = new WebSocketMessage();
        webSocketMessage.setCode(GROUP_CHAT_MESSAGE_CODE);
        webSocketMessage.setMessage(chat);
        webSocketMessage.setTime(DateUtils.date2String(new Date()));
        webSocketMessage.setUser(user);
        return JSONObject.toJSONString(webSocketMessage);
    }

    public String getPrivateChatMessageJSONString(User user, String id, String chat) {
        WebSocketMessage webSocketMessage = new WebSocketMessage();
        webSocketMessage.setCode(PRIVATE_CHAT_MESSAGE_CODE);
        webSocketMessage.setMessage(chat);
        webSocketMessage.setTime(DateUtils.date2String(new Date()));
        webSocketMessage.setUser(user);
        webSocketMessage.setReceiverId(id);
        return JSONObject.toJSONString(webSocketMessage);
    }

    public String getPersonalSystemMessageJSONString(User user) {
        WebSocketMessage webSocketMessage = new WebSocketMessage();
        webSocketMessage.setCode(SYSTEM_MESSAGE_CODE);
        Map<String, Object> map = new HashMap<>();
        map.put("systemMessageCode", PERSONAL_SYSTEM_MESSGAE_CODE);
        map.put("user", user);
        webSocketMessage.setBody(map);
        return JSONObject.toJSONString(webSocketMessage);
    }
}
