package com.abc.wechat.service.Impl;

import com.abc.wechat.dao.db1.ChatRecordsMapper;
import com.abc.wechat.dao.db2.ContactsMapper;
import com.abc.wechat.dto.Msg;
import com.abc.wechat.entity.ChatMsg;
import com.abc.wechat.service.ChatRecordsService;
import com.abc.wechat.utils.StrUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatRecordsServiceImpl implements ChatRecordsService {
    @Autowired
    private ChatRecordsMapper chatRecordsMapper;
    @Autowired
    private ContactsMapper contactsMapper;

    @Override
    public List<ChatMsg> selectAllChatRecords() {
        return chatRecordsMapper.selectAllMsg();
    }

    @Override
    public List<Msg> selectAllMsgs(){
        List<Msg> msgList = new ArrayList<>();
        List<ChatMsg> chatMsgList = selectAllChatRecords();
        for(ChatMsg chatMsg : chatMsgList){
            Msg msg = new Msg();
            msg.setId(chatMsg.getId());
            msg.setTime(StrUtils.createTime(chatMsg.getCreateTime()));
            msg.setContent(chatMsg.getContent());
            msg.setType(StrUtils.type(chatMsg.getType()));
            if(chatMsg.getIsSender() == 1) {
                msg.setFrom("我");
                msg.setGroup("");
            }else{
                if(!StrUtils.isGroupChat(chatMsg.getSender())) {
                    msg.setFrom(contactsMapper.selectNameById(chatMsg.getSender()));
                    msg.setGroup("");
                }else{
                    msg.setFrom(contactsMapper.selectNameById(StrUtils.wxId(chatMsg.getExtra())));
                    msg.setGroup(contactsMapper.selectNameById(chatMsg.getSender()));
                }
            }
            msgList.add(msg);
        }
        return msgList;
    }
}
