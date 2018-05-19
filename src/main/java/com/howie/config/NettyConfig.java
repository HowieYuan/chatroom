package com.howie.config;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * Created with IntelliJ IDEA
 *
 * @Author yuanhaoyue swithaoy@gmail.com
 * @Description netty 全局配置
 * @Date 2018-05-07
 * @Time 15:11
 */
public class NettyConfig {
    /**
     * 存储每个客户端接入进来时的 channel 对象
     * 主要用于使用 writeAndFlush 方法广播信息
     */
    public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
}
