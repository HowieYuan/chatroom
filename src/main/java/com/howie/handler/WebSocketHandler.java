package com.howie.handler;

import com.alibaba.fastjson.JSONObject;
import com.howie.config.NettyConfig;
import com.howie.constant.WebSocketConstant;
import com.howie.model.User;
import com.howie.service.MessageService;
import com.howie.service.WebSocketInfoService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.util.Objects;
import java.util.UUID;

import static com.howie.constant.MessageCodeConstant.*;


/**
 * Created with IntelliJ IDEA
 *
 * @Author yuanhaoyue swithaoy@gmail.com
 * @Description
 * @Date 2018-05-07
 * @Time 15:41
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {
    private WebSocketServerHandshaker handshaker;
    private WebSocketInfoService webSocketInfoService = new WebSocketInfoService();
    private MessageService messageService = new MessageService();

    /**
     * 客户端与服务端创建连接的时候调用
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        webSocketInfoService.addChannel(ctx.channel());
        System.out.println("————客户端与服务端连接开启————");
    }

    /**
     * 客户端与服务端断开连接的时候调用
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("————客户端与服务端连接断开————");
    }

    /**
     * 服务端接收客户端发送过来的数据结束之后调用
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("—————channelReadComplete————");
        ctx.flush();
    }

    /**
     * 工程出现异常的时候调用
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 服务端处理客户端websocket请求的核心方法
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        System.out.println("—————channelRead0————");
        if (o instanceof FullHttpRequest) {
            //处理客户端向服务端发起 http 握手请求的业务
            handHttpRequest(channelHandlerContext, (FullHttpRequest) o);
        } else if (o instanceof WebSocketFrame) {
            //处理客户端与服务端之间的 websocket 业务
            handWebsocketFrame(channelHandlerContext, (WebSocketFrame) o);
        }
    }

    /**
     * 处理客户端向服务端发起 http 握手请求的业务
     */
    private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        System.out.println("—————handHttpRequest————");
        // 如果请求失败或者该请求不是客户端向服务端发起的 http 握手请求，则响应错误信息
        if (!request.decoderResult().isSuccess()
                || !("websocket".equals(request.headers().get("Upgrade")))) {
            // code ：400
            sendHttpResponse(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        // WebSocket 握手工厂类
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(
                WebSocketConstant.WEB_SOCKET_URL, null, false);
        //新建一个握手
        handshaker = factory.newHandshaker(request);
        if (handshaker == null) {
            //如果为空，返回不能支持 websocket 版本的响应
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            //否则，执行握手
            handshaker.handshake(ctx.channel(), request);
        }
    }

    /**
     * 处理客户端与服务端之间的 websocket 业务
     */
    private void handWebsocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        System.out.println("—————handWebsocketFrame————");
        //判断是否是关闭 websocket 的指令
        if (frame instanceof CloseWebSocketFrame) {
            //关闭握手
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            webSocketInfoService.deleteChannel(ctx.channel());
            return;
        }
        //判断是否是ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        //判断是否是二进制消息，如果是二进制消息，抛出异常
        if (!(frame instanceof TextWebSocketFrame)) {
            System.out.println("目前我们不支持二进制消息");
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            throw new RuntimeException("【" + this.getClass().getName() + "】不支持消息");
        }
        //返回应答消息
        // 获取客户端向服务端发送的消息
        String message = ((TextWebSocketFrame) frame).text();
        JSONObject json = JSONObject.parseObject(message);
        int code = json.getInteger("code");
        String nick = json.getString("nick");
        User user = WebSocketInfoService.webSocketInfoMap.get(ctx.channel());
        String chatMessage = json.getString("chatMessage");
        TextWebSocketFrame tws;
        switch (code) {
            case LOGIN_CODE:
                String id = UUID.randomUUID().toString();
                if (!webSocketInfoService.addUser(ctx.channel(), nick, id)) {
                    return;
                }
                webSocketInfoService.updateUserListAndCount();
                tws = new TextWebSocketFrame(messageService
                        .getSystemMessageJSONString("欢迎" + nick + "来到聊天室~", NORMAL_SYSTEM_MESSGAE_CODE));
                //群发，服务端向每个连接上来的客户端群发消息
                NettyConfig.channelGroup.writeAndFlush(tws);
                tws = new TextWebSocketFrame(messageService.getPersonalSystemMessageJSONString(user));
                ctx.channel().writeAndFlush(tws);
                break;
            case GROUP_CHAT_CODE:
                tws = new TextWebSocketFrame(messageService
                        .getGroupChatMessageJSONString(user, chatMessage));
                //群发，服务端向每个连接上来的客户端群发消息
                NettyConfig.channelGroup.writeAndFlush(tws);
                break;
            case PRIVATE_CHAT_CODE:
                Channel myChannel = ctx.channel();
                //接收人id
                String receiverId = json.getString("id");
                //发送人id
                String senderId = user.getId();
                //发给对方
                tws = new TextWebSocketFrame(messageService
                        .getPrivateChatMessageJSONString(user, senderId, chatMessage));
                webSocketInfoService.sendPrivateChatMessage(receiverId, tws);
                //如果不是发给自己
                if (!Objects.equals(receiverId, senderId)) {
                    //再发给自己
                    tws = new TextWebSocketFrame(messageService
                            .getPrivateChatMessageJSONString(user, receiverId,
                                    nick + ": " + chatMessage));
                    myChannel.writeAndFlush(tws);
                }

            default:
        }
        System.out.println("服务端收到客户端的消息====>>>" + message);
    }

    /**
     * 服务端向客户端响应消息
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, DefaultFullHttpResponse response) {
        System.out.println("—————sendHttpResponse————");
        if (response.status().code() != 200) {
            //创建源缓冲区
            ByteBuf byteBuf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
            //将源缓冲区的数据传送到此缓冲区
            response.content().writeBytes(byteBuf);
            //释放源缓冲区
            byteBuf.release();
        }
        //写入请求，服务端向客户端发送数据
        ChannelFuture channelFuture = ctx.channel().writeAndFlush(response);
        if (response.status().code() != 200) {
            /*
                如果请求失败，关闭 ChannelFuture

                ChannelFutureListener.CLOSE 源码：future.channel().close();
             */
            channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
