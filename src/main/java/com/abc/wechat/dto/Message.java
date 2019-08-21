package com.abc.wechat.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class Message implements Serializable {
    private String headImgUlr;
    private Date recv_time;
    private String type;
    private String detail;
    private Long recv_timestamp;
    private String gid;
    private String gname; // group name
    private String send_user;      //nick name
    private String send_username; //wxid
    private UUID rid;//绑定者微信id
    private String uid;//当前用户id
    private UUID id;//消息id
}
