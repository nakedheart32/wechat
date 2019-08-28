package com.abc.wechat.service;

import com.abc.wechat.dto.Message;
import com.abc.wechat.dto.Msg;
import com.abc.wechat.entity.ChatMsg;

import java.util.List;

public interface ChatRecordsService {

    public List<ChatMsg> selectChatMsg();
    public List<Msg> selectTestMsg();
    public List<Message> selectMessages();

    List<Integer> upload();
    public List<Integer> reupload();

}
