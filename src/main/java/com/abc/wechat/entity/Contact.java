package com.abc.wechat.entity;

import lombok.Data;

@Data
public class Contact {
    private String userName;
    private String remark;
    private String nickName;
    private Boolean isChatRoom;

}
