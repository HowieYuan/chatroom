package com.howie.service;

import com.howie.config.NettyConfig;
import com.howie.model.User;
import com.howie.util.WebSocketUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.howie.constant.MessageCodeConstant.NORMAL_SYSTEM_MESSGAE_CODE;
import static com.howie.constant.MessageCodeConstant.UPDATE_USERCOUNT_SYSTEM_MESSGAE_CODE;
import static com.howie.constant.MessageCodeConstant.UPDATE_USERLIST_SYSTEM_MESSGAE_CODE;

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
    private static ConcurrentMap<Channel, User> webSocketInfoMap = new ConcurrentHashMap<Channel, User>();
    /**
     * 用户在线数量
     */
    private static AtomicInteger userCount = new AtomicInteger(0);

    public void addChannel(Channel channel) {
        User user = new User();
        user.setAddress(WebSocketUtil.getChannelAddress(channel));
        webSocketInfoMap.put(channel, user);
        NettyConfig.channelGroup.add(channel);
    }

    public void deleteChannel(Channel channel) {
        String nick = webSocketInfoMap.get(channel).getNick();
        webSocketInfoMap.remove(channel);
        userCount.decrementAndGet();
        NettyConfig.channelGroup.remove(channel);
        TextWebSocketFrame tws = new TextWebSocketFrame(new MessageService()
                .getSystemMessageJSONString(nick + "离开了聊天室~", NORMAL_SYSTEM_MESSGAE_CODE));
        new WebSocketInfoService().updateUserListAndCount();
        NettyConfig.channelGroup.writeAndFlush(tws);
    }

    public void updateUserListAndCount() {
        //更新在线人数
        TextWebSocketFrame tws = new TextWebSocketFrame(new MessageService()
                .getSystemMessageJSONString(null, UPDATE_USERCOUNT_SYSTEM_MESSGAE_CODE, userCount));
        NettyConfig.channelGroup.writeAndFlush(tws);

        //更新在线用户列表
        List<User> userList = new ArrayList<>();
        Set<Channel> set = webSocketInfoMap.keySet();
        for (Channel channel: set) {
            User user = webSocketInfoMap.get(channel);
            userList.add(user);
        }
        tws = new TextWebSocketFrame(new MessageService()
                .getSystemMessageJSONString(null, UPDATE_USERLIST_SYSTEM_MESSGAE_CODE, userList));
        NettyConfig.channelGroup.writeAndFlush(tws);
    }

    public boolean addUser(Channel channel, String nick) {
        User user = webSocketInfoMap.get(channel);
        if (user == null) {
            return false;
        }
        user.setNick(nick);
        user.setLoginTime(System.currentTimeMillis());
        userCount.incrementAndGet();
        return true;
    }
}
