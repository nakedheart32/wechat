package com.abc.wechat.utils;

import lombok.Data;

@Data
public class ResultMsg {
    private int code;
    private String msg;
    private Object data;
}
