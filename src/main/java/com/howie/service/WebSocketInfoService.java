package com.howie.service;

import com.alibaba.fastjson.JSONObject;
import com.howie.config.NettyConfig;
import com.howie.model.User;
import com.howie.model.WebSocketMessage;
import com.howie.util.WebSocketUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.howie.constant.MessageCodeConstant.*;

/**
 * Created with IntelliJ IDEA
 *
 * @Author yuanhaoyue swithaoy@gmail.com
 * @Description
 * @Date 2018-05-11
 * @Time 20:56
 */
public class WebSocketInfoService {
    /**
     * 存储 Channel 与用户信息
     */
    public static ConcurrentMap<Channel, User> webSocketInfoMap = new ConcurrentHashMap<>();
    /**
     * 用户在线数量
     */
    private static AtomicInteger userCount = new AtomicInteger(0);

    /**
     * 新的客户端与服务端进行连接，先保存新的 channel，
     * 当连接建立后，客户端会发送用户登陆请求（LOGIN_CODE），这时再将用户信息保存进去
     */
    public void addChannel(Channel channel) {
        User user = new User();
        user.setAddress(WebSocketUtil.getChannelAddress(channel));
        webSocketInfoMap.put(channel, user);
        NettyConfig.channelGroup.add(channel);
    }

    /**
     * 有用户退出，需要删除该用户的信息，并移除该用户的 channel
     */
    public void deleteChannel(Channel channel) {
        String nick = webSocketInfoMap.get(channel).getNick();
        webSocketInfoMap.remove(channel);
        userCount.decrementAndGet();
        NettyConfig.channelGroup.remove(channel);
        //广播用户离开的信息
        TextWebSocketFrame tws = new TextWebSocketFrame(new MessageService().messageJSONStringFactory(SYSTEM_MESSAGE_CODE,
                nick + "离开了聊天室~", NORMAL_SYSTEM_MESSGAE_CODE, null));
        new WebSocketInfoService().updateUserListAndCount();
        NettyConfig.channelGroup.writeAndFlush(tws);
    }

    /**
     * 向服务端发送信息，携带新的在线人数/携带新的用户列表
     */
    public void updateUserListAndCount() {
        //更新在线人数
        TextWebSocketFrame tws = new TextWebSocketFrame(new MessageService().messageJSONStringFactory(SYSTEM_MESSAGE_CODE,
                null, UPDATE_USERCOUNT_SYSTEM_MESSGAE_CODE, userCount));
        NettyConfig.channelGroup.writeAndFlush(tws);

        //更新在线用户列表
        List<User> userList = new ArrayList<>();
        Set<Channel> set = webSocketInfoMap.keySet();
        for (Channel channel : set) {
            User user = webSocketInfoMap.get(channel);
            userList.add(user);
        }
        tws = new TextWebSocketFrame(new MessageService().messageJSONStringFactory(SYSTEM_MESSAGE_CODE,
                null, UPDATE_USERLIST_SYSTEM_MESSGAE_CODE, userList));
        NettyConfig.channelGroup.writeAndFlush(tws);
    }

    /**
     * 将 nick，id,avatarAddress 等用户信息保存到对应的 channel 的 value 中
     *
     * @param channel 属于某用户的 channel
     * @param nick    昵称
     * @param id      用户 id
     * @return 如果当前用户不存在，则返回 false
     */
    public boolean addUser(Channel channel, String nick, String id) {
        User user = webSocketInfoMap.get(channel);
        if (user == null) {
            return false;
        }
        user.setId(id);
        user.setNick(nick);
        user.setAvatarAddress(getRandomAvatar());
        user.setTime(System.currentTimeMillis());
        //用户在线数量 + 1
        userCount.incrementAndGet();
        return true;
    }

    /**
     * 返回一个随机的头像地址
     */
    private String getRandomAvatar() {
        int num = new Random().nextInt(33) + 1;
        return "../img/" + num + ".png";
    }

    /**
     * 发送私聊信息
     *
     * @param id  收信人id
     * @param tws
     */
    public void sendPrivateChatMessage(String id, TextWebSocketFrame tws) {
        Set<Channel> set = webSocketInfoMap.keySet();
        Channel receiverChannel = null;
        for (Channel channel : set) {
            User user = webSocketInfoMap.get(channel);
            if (user.getId().equals(id)) {
                receiverChannel = channel;
                break;
            }
        }
        if (receiverChannel != null) {
            receiverChannel.writeAndFlush(tws);
        }
    }

    /**
     * 广播 ping 信息
     */
    public void sendPing() {
        Set<Channel> keySet = webSocketInfoMap.keySet();
        for (Channel channel : keySet) {
            User user = webSocketInfoMap.get(channel);
            if (user == null) {
                continue;
            }
            WebSocketMessage webSocketMessage = new WebSocketMessage();
            webSocketMessage.setCode(PING_MESSAGE_CODE);
            String message = JSONObject.toJSONString(webSocketMessage);
            TextWebSocketFrame tws = new TextWebSocketFrame(message);
            NettyConfig.channelGroup.writeAndFlush(tws);
        }
    }

    /**
     * 从缓存中移除Channel，并且关闭Channel
     */
    public void scanNotActiveChannel() {
        Set<Channel> keySet = webSocketInfoMap.keySet();
        for (Channel channel : keySet) {
            User user = webSocketInfoMap.get(channel);
            if (user == null) {
                continue;
            }
            if (!channel.isOpen() || !channel.isActive() &&
                    (System.currentTimeMillis() - user.getTime()) > 10000) {
                deleteChannel(channel);
            }
        }
    }

    /**
     * 重设验证在线时间
     */
    public void resetUserTime(Channel channel) {
        User user = webSocketInfoMap.get(channel);
        if (user != null) {
            user.setTime(System.currentTimeMillis());
        }
    }
}
