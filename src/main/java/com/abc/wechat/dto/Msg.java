package com.abc.wechat.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Msg implements Serializable {
    private Integer id;
    private String time;
    private String from;
    private String group;
    private String type;
    private String content;
}
