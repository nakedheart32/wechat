package com.abc.wechat.conroller;

import com.abc.wechat.dao.db1.ChatRecordsMapper;
import com.abc.wechat.dao.db2.ContactsMapper;
import com.abc.wechat.dto.Msg;
import com.abc.wechat.entity.ChatMsg;
import com.abc.wechat.service.ChatRecordsService;
import com.abc.wechat.utils.ResultMsg;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class MainController {


    @Autowired
    private ChatRecordsService chatRecordsService;
    @Autowired
    private ContactsMapper contactsMapper;
    @Autowired
    private ChatRecordsMapper chatRecordsMapper;

    @GetMapping("/chatRecords")
    public ResultMsg selectAllChatRecords(){
        List<ChatMsg> chatMsgs = chatRecordsService.selectAllChatRecords();
        JSONArray json = JSONArray.parseArray(JSON.toJSONString(chatMsgs));
        return responseResultMsg(200, "success", json);
    }

    @GetMapping("/msgs")
    public ResultMsg selectAllMsgs(){
        List<Msg> msgList = chatRecordsService.selectAllMsgs();
        JSONArray json = JSONArray.parseArray((JSON.toJSONString(msgList)));
        return responseResultMsg(200, "success", json);
    }

    private ResultMsg responseResultMsg(int code, String msg, Object data){
        ResultMsg resultMsg = new ResultMsg();
        resultMsg.setCode(code);
        resultMsg.setMsg(msg);
        resultMsg.setData(data);
        return resultMsg;
    }

    @GetMapping("/test")
    public String test(){
        chatRecordsMapper.testCache((int)(Math.random() * 100));
        return "ok";
    }

}
