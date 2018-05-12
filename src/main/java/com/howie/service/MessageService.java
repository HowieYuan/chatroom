package com.howie.service;

import com.alibaba.fastjson.JSONObject;
import com.howie.model.WebSocketMessage;
import com.howie.util.DateUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.howie.constant.MessageCodeConstant.CHAT_MESSAGE_CODE;
import static com.howie.constant.MessageCodeConstant.SYSTEM_MESSAGE_CODE;

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

    public String getChatMessageJSONString(String nick, String chat) {
        WebSocketMessage webSocketMessage = new WebSocketMessage();
        webSocketMessage.setCode(CHAT_MESSAGE_CODE);
        webSocketMessage.setMessage(chat);
        webSocketMessage.setTime(DateUtils.date2String(new Date()));
        webSocketMessage.setNick(nick);
        return JSONObject.toJSONString(webSocketMessage);
    }
}
