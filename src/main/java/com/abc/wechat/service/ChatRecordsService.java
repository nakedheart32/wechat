package com.abc.wechat.service;

import com.abc.wechat.dto.Message;

import java.util.List;

public interface ChatRecordsService {

    //List<Message> selectMessages();
    List<Integer> upload();
    List<Integer> reupload();

}
