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
 * @Description 接收请求，接收 WebSocket 信息的控制类
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
        //创建新的 WebSocket 连接，保存当前 channel
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
        if (o instanceof FullHttpRequest) {
            //处理客户端向服务端发起 http 请求的业务
            handHttpRequest(channelHandlerContext, (FullHttpRequest) o);
        } else if (o instanceof WebSocketFrame) {
            //处理客户端与服务端之间的 websocket 业务
            handWebsocketFrame(channelHandlerContext, (WebSocketFrame) o);
        }
    }

    /**
     * 处理客户端向服务端发起 http 握手请求的业务
     * WebSocket在建立握手时，数据是通过HTTP传输的。但是建立之后，在真正传输时候是不需要HTTP协议的。
     *
     * WebSocket 连接过程：
     * 首先，客户端发起http请求，经过3次握手后，建立起TCP连接；http请求里存放WebSocket支持的版本号等信息，如：Upgrade、Connection、WebSocket-Version等；
     * 然后，服务器收到客户端的握手请求后，同样采用HTTP协议回馈数据；
     * 最后，客户端收到连接成功的消息后，开始借助于TCP传输信道进行全双工通信。
     */
    private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 如果请求失败或者该请求不是客户端向服务端发起的 http 请求，则响应错误信息
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
            //如果为空，返回响应：不受支持的 websocket 版本
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
        // 判断是否Pong消息
        if (frame instanceof PongWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        //判断是否是二进制消息，如果是二进制消息，抛出异常
        if (!(frame instanceof TextWebSocketFrame)) {
            System.out.println("目前我们不支持二进制消息");
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            throw new RuntimeException("【" + this.getClass().getName() + "】不支持消息");
        }
        // 返回应答消息
        // 获取并解析客户端向服务端发送的 json 消息
        String message = ((TextWebSocketFrame) frame).text();
        JSONObject json = JSONObject.parseObject(message);
        int code = json.getInteger("code");
        String nick = json.getString("nick");
        String chatMessage = json.getString("chatMessage");
        //从 webSocketInfoMap 拿出属于当前 channel 的 User 信息
        User user = WebSocketInfoService.webSocketInfoMap.get(ctx.channel());
        TextWebSocketFrame tws;
        switch (code) {
            //用户登陆
            case LOGIN_CODE:
                //生成 UUID 作为用户 id
                String id = UUID.randomUUID().toString();
                if (!webSocketInfoService.addUser(ctx.channel(), nick, id)) {
                    return;
                }
                webSocketInfoService.updateUserListAndCount();
                //向每个连接上来的客户端群发新用户登陆信息
                tws = new TextWebSocketFrame(messageService.messageJSONStringFactory(SYSTEM_MESSAGE_CODE,
                        "欢迎" + nick + "来到聊天室~", NORMAL_SYSTEM_MESSGAE_CODE, null));
                NettyConfig.channelGroup.writeAndFlush(tws);
                //再向当前登陆的客户端发送该用户的用户信息，用于前端使用
                tws = new TextWebSocketFrame(messageService.messageJSONStringFactory(SYSTEM_MESSAGE_CODE, null,
                        PERSONAL_SYSTEM_MESSGAE_CODE, user));
                ctx.channel().writeAndFlush(tws);
                break;
            //群聊
            case GROUP_CHAT_CODE:
                //向每个连接上来的客户端群发群聊消息
                tws = new TextWebSocketFrame(messageService.messageJSONStringFactory(GROUP_CHAT_MESSAGE_CODE, chatMessage,
                        user, null));
                NettyConfig.channelGroup.writeAndFlush(tws);
                break;
            //私聊
            case PRIVATE_CHAT_CODE:
                Channel myChannel = ctx.channel();
                //接收人id
                String receiverId = json.getString("id");
                //发送人id
                String senderId = user.getId();
                /*
                    向目标用户发送私聊信息，发送人 id 为 senderId
                 */
                tws = new TextWebSocketFrame(messageService.messageJSONStringFactory(PRIVATE_CHAT_MESSAGE_CODE, chatMessage,
                        user, senderId));
                webSocketInfoService.sendPrivateChatMessage(receiverId, tws);
                /*
                    如果当前信息是自己发给自己，那么信息只需要发给自己就好
                    但是如果是发给他人，则信息既要发给对方，也要发给自己（两边都要显示）
                 */
                if (!Objects.equals(receiverId, senderId)) {
                    //发给自己，发送人 id 为 receiverId
                    tws = new TextWebSocketFrame(messageService.messageJSONStringFactory(PRIVATE_CHAT_MESSAGE_CODE, chatMessage,
                            user, receiverId));
                    myChannel.writeAndFlush(tws);
                }
                break;
            //pong
            case PONG_CHAT_CODE:
                Channel channel = ctx.channel();
                webSocketInfoService.resetUserTime(channel);
            default:
        }
    }

    /**
     * 服务端向客户端响应消息
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, DefaultFullHttpResponse response) {
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
