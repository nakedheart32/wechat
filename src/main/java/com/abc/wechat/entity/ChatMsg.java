package com.abc.wechat.entity;

import lombok.Data;

@Data
public class ChatMsg {
    private Integer id;
    private Integer type; // 1 for txt 、3 for image、 49 for file
    private Integer isSender;  // 1 for yes 、 0 for no
    private String createTime; // unix timeStamp
    private String sender;   // wechat id of  chatMsg, like "wxid_8rgje3s43f4r22"、"9249272246@chatroom"
    private String content;
    private String extra;

    @Override
    public String toString() {
        return "ChatMsg{" +
                "id=" + id +
                ", type=" + type +
                ", isSender=" + isSender +
                ", createTime='" + createTime + '\'' +
                ", sender='" + sender + '\'' +
                ", content='" + content + '\'' +
                ", extra='" + extra + '\'' +
                '}';
    }
}
