let socket;
const groupChatName = "群聊室";
let currentChatUserNick = groupChatName;
let currentChatUserId;

const webSocketUrl = "ws://localhost:8090/websocket";
const myNick = GetQueryString("nick");
let me;

const GROUP_CHAT_MESSAGE_CODE = 2000;
const SYSTEM_MESSAGE_CODE = 2001;
const PRIVATE_CHAT_MESSAGE_CODE = 2002;
const PING_MESSAGE_CODE = 2003;

const NORMAL_SYSTEM_MESSGAE_CODE = 3000;
const UPDATE_USERCOUNT_SYSTEM_MESSGAE_CODE = 3001;
const UPDATE_USERLIST_SYSTEM_MESSGAE_CODE = 3002;
const PERSONAL_SYSTEM_MESSGAE_CODE = 3003;

function systemMessage(data) {
    switch (data.body.systemMessageCode) {
        case PING_MESSAGE_CODE :
            sendPong();
        case NORMAL_SYSTEM_MESSGAE_CODE :
            $("#responseContent").append("<div class='systemMessage'>" + data.message + " (" + data.time + ")" + "</div>");
            break;
        case PERSONAL_SYSTEM_MESSGAE_CODE:
            me = data.body.object;
            $("#myAvatar").attr("src", me.avatarAddress);
            break;
        case UPDATE_USERCOUNT_SYSTEM_MESSGAE_CODE :
            $('#userCount').text("在线人数：" + data.body.object);
            break;
        case UPDATE_USERLIST_SYSTEM_MESSGAE_CODE :
            let users = data.body.object;
            let userList = $("#userList");
            let repeatBox = $("#repeatBox");
            let appendString;
            userList.text("");
            userList.append(
                '<div class="chat_item" onClick="chooseUser(null, null)" style="z-index: ">' +
                '<img class="avatar img-circle" src="../img/chatroom.png" style="height: 50px;width: 50px">' +
                '<img id="redPoint" class="img-circle" src="../img/redPoint.png" style="height: 10px;width: 10px;position: absolute;left: 60;display: none">' +
                '<div style="color: white;font-size: large">群聊室</div>' +
                '</div>');
            users.forEach(function (user) {
                userList.append(
                    '<div class="chat_item" onClick="chooseUser(\'' + user.nick + '\',\'' + user.id + '\')">' +
                    '<img class="avatar img-circle" src=' + user.avatarAddress + ' style="height: 50px;width: 50px">' +
                    '<img id="redPoint-' + user.id + '" class="img-circle" src="../img/redPoint.png" style="height: 10px;width: 10px;position: absolute;left: 60;display: none">' +
                    '<div style="color: white;font-size: large">' + user.nick + '</div>' +
                    '</div>');
                appendString =
                    ['<div class="box" id="box-' + user.id + '" style="display: none">',
                        '    <div class="textareaHead" id="textareaHead">' + user.nick + '</div>',
                        '    <div class="textarea scroll" id="responseContent-' + user.id + '"></div>',
                        '    <form onSubmit="return false;">',
                        '        <label>',
                        '            <textarea class="box_ft" name="message" id="sendTextarea-' + user.id + '"></textarea>',
                        '        </label>',
                        '        <div class="send"><button class="sendButton" onClick="sendMessageToUser(this.form.message.value, currentChatUserId)">发送</button></div>',
                        '    </form>',
                        '</div>'].join("");
                repeatBox.append(appendString);
            });
            break;
    }
}

function websocket() {
    if (!window.WebSocket) {
        window.WebSocket = window.MozWebSocket;
    }
    if (window.WebSocket) {
        socket = new WebSocket(webSocketUrl);
        socket.onmessage = function (event) {
            let data = JSON.parse(event.data);
            switch (data.code) {
                case GROUP_CHAT_MESSAGE_CODE:
                    if (data.user.id !== me.id) {
                        $("#responseContent").append(
                            "   <div class='chatMessageBox'>" +
                            "       <img class='chatAvatar' src=" + data.user.avatarAddress + ">" +
                            "       <div class='chatTime'>" + data.user.nick + "&nbsp;&nbsp; " + data.time + "</div>" +
                            "       <div class='chatMessgae'><span>" + data.message + "</span></div>" +
                            "   </div>");
                    } else {
                        $("#responseContent").append(
                            "   <div class='chatMessageBox_me'>" +
                            "       <img class='chatAvatar_me' src=" + data.user.avatarAddress + ">" +
                            "       <div class='chatTime'>" + data.time + "&nbsp;&nbsp; " + data.user.nick + "</div>" +
                            "       <div class='chatMessgae_me'><span>" + data.message + "</span></div>" +
                            "   </div>");
                    }
                    updateRedPoint(null);
                    boxScroll(document.getElementById("responseContent"));
                    break;
                case SYSTEM_MESSAGE_CODE:
                    systemMessage(data);
                    boxScroll(document.getElementById("responseContent"));
                    break;
                case PRIVATE_CHAT_MESSAGE_CODE:
                    if (data.user.id !== me.id) {
                        $("#responseContent-" + data.receiverId).append(
                            "   <div class='chatMessageBox'>" +
                            "       <img class='chatAvatar' src=" + data.user.avatarAddress + ">" +
                            "       <div class='chatTime'>" + data.user.nick + "&nbsp;&nbsp;  " + data.time + "</div>" +
                            "       <div class='chatMessgae'><span>" + data.message + "</span></div>" +
                            "   </div>");
                    } else {
                        $("#responseContent-" + data.receiverId).append(
                            "   <div class='chatMessageBox_me'>" +
                            "       <img class='chatAvatar_me' src=" + data.user.avatarAddress + ">" +
                            "       <div class='chatTime'>" + data.time + "&nbsp;&nbsp; " + data.user.nick + "</div>" +
                            "       <div class='chatMessgae_me'><span>" + data.message + "</span></div>" +
                            "   </div>");
                    }
                    updateRedPoint(data.user.id);
                    boxScroll(document.getElementById("responseContent-" + data.receiverId));
            }
        };
        socket.onopen = function () {
            loginSend();
        };
        socket.onclose = function () {
            quitSend();
        };
        return true;
    } else {
        alert("您的浏览器不支持WebSocket");
        return false;
    }
}

function loginSend() {
    let object = {};
    object.code = 1000;
    object.nick = myNick;
    send(JSON.stringify(object));
}

function quitSend() {
    let object = {};
    object.code = 1001;
    object.nick = myNick;
    send(JSON.stringify(object));
}


function sendPong() {
    let object = {};
    object.code = 1004;
    send(JSON.stringify(object));
}

function sendMessageToUser(message, id) {
    if (message === "" || message == null) {
        alert("信息不能为空~");
        return;
    }
    let object = {};
    object.code = 1003;
    object.nick = myNick;
    object.id = id;
    object.chatMessage = message;
    $('#sendTextarea-' + id).val("");
    send(JSON.stringify(object));
}

function sendMessage(message) {
    if (message === "" || message == null) {
        alert("信息不能为空~");
        return;
    }
    let object = {};
    object.code = 1002;
    object.nick = myNick;
    object.chatMessage = message;
    $('#sendTextarea').val("");
    send(JSON.stringify(object));
}

function send(message) {
    if (!window.WebSocket) {
        return;
    }
    if (socket.readyState === WebSocket.OPEN) {
        socket.send(message);
    } else {
        alert("WebSocket连接没有建立成功！！");
    }
}

function chooseUser(username, id) {
    let box = $("#box");
    if (username != null) {
        $("#redPoint-" + id).css("display", "none");
        if (currentChatUserNick === groupChatName) {
            $("#box-" + id).css("display", "block");
            box.css("display", "none");
            currentChatUserNick = username;
            currentChatUserId = id;
        } else if (currentChatUserNick !== groupChatName && currentChatUserId !== id) {
            $("#box-" + id).css("display", "block");
            $("#box-" + currentChatUserId).css("display", "none");
            currentChatUserNick = username;
            currentChatUserId = id;
        }
    } else if (username === null && currentChatUserNick !== groupChatName) {
        $("#redPoint").css("display", "none");
        $("#box-" + id).css("display", "none");
        box.css("display", "block");
        currentChatUserNick = groupChatName;
    }
}

/**
 * 新消息红点提醒
 * @param id
 */
function updateRedPoint(id) {
    if (id == null && currentChatUserNick !== groupChatName) {
        $("#redPoint").css("display", "block");
    } else if (currentChatUserId !== id && id !== me.id) {
        $("#redPoint-" + id).css("display", "block");
    }
}

/**
 * Get 请求获取参数
 * @return {null}
 * @param name 参数名
 */
function GetQueryString(name) {
    const reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    const r = window.location.search.substr(1).match(reg);
    if (r !== null) {
        return unescape(decodeURI(decodeURI(r[2])));
    }
    return null;
}

/**
 * 滚动条置底
 * @param o document.getElementById("id")
 */
function boxScroll(o) {
    o.scrollTop = o.scrollHeight;
}