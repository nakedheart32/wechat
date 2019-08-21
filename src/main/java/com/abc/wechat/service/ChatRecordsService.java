package com.abc.wechat.service;

import com.abc.wechat.dto.Message;
import com.abc.wechat.dto.Msg;
import com.abc.wechat.entity.ChatMsg;

import java.util.List;

public interface ChatRecordsService {
    public List<ChatMsg> selectAllChatRecords();
    public List<Msg> selectAllMsgs();
    public List<ChatMsg> selectRecords();
    public List<Msg> selectMsgs();
    public List<Message> selectMessages();
}
