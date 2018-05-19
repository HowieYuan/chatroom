package com.howie.util;

import io.netty.channel.Channel;

import java.net.SocketAddress;

/**
 * Created with IntelliJ IDEA
 *
 * @Author yuanhaoyue swithaoy@gmail.com
 * @Description
 * @Date 2018-05-12
 * @Time 17:15
 */
public class WebSocketUtil {
    /**
     * 获得Channel远程主机IP地址
     */
    public static String getChannelAddress(final Channel channel) {
        if (null == channel) {
            return "";
        }
        SocketAddress address = channel.remoteAddress();
        String addr = (address != null ? address.toString() : "");
        int index = addr.lastIndexOf("/");
        if (index >= 0) {
            return addr.substring(index + 1);
        }
        return addr;
    }
}
