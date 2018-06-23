package com.howie.constant;

/**
 * Created with IntelliJ IDEA
 *
 * @Author yuanhaoyue swithaoy@gmail.com
 * @Description Code 常量
 * @Date 2018-05-12
 * @Time 16:53
 */
public class MessageCodeConstant {
    /*
      CODE
      客户端信息传送 code
     */
    /**
     * 登陆
     */
    public static final int LOGIN_CODE = 1000;
    /**
     * 群聊
     */
    public static final int GROUP_CHAT_CODE = 1002;
    /**
     * 私聊
     */
    public static final int PRIVATE_CHAT_CODE = 1003;
    /**
     * pong 信息
     */
    public static final int PONG_CHAT_CODE = 1004;


    /*
      MESSAGE_CODE
      服务端信息传送 code
     */
    /**
     * 群聊信息
     */
    public static final int GROUP_CHAT_MESSAGE_CODE = 2000;
    /**
     * 系统信息
     */
    public static final int SYSTEM_MESSAGE_CODE = 2001;
    /**
     * 私聊信息
     */
    public static final int PRIVATE_CHAT_MESSAGE_CODE = 2002;
    /**
     * ping 信息
     */
    public static final int PING_MESSAGE_CODE = 2003;


    /*
      SYSTEM_MESSGAE_CODE
     */
    /**
     * 普通系统信息：用户上线，下线广播通知等
     */
    public static final int NORMAL_SYSTEM_MESSGAE_CODE = 3000;
    /**
     * 更新当前用户数量的系统信息
     */
    public static final int UPDATE_USERCOUNT_SYSTEM_MESSGAE_CODE = 3001;
    /**
     * 更新当前用户列表的系统信息
     */
    public static final int UPDATE_USERLIST_SYSTEM_MESSGAE_CODE = 3002;
    /**
     * 获取个人信息的系统信息
     */
    public static final int PERSONAL_SYSTEM_MESSGAE_CODE = 3003;
}
