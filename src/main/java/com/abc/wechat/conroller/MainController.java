package com.abc.wechat.conroller;

import com.abc.wechat.dao.db1.ChatRecordsMapper;
import com.abc.wechat.dao.db2.ContactsMapper;
import com.abc.wechat.dto.Message;
import com.abc.wechat.dto.Msg;
import com.abc.wechat.service.ChatRecordsService;
import com.abc.wechat.utils.ResultMsg;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/messages/text")
public class MainController {
    @Autowired
    private ChatRecordsService chatRecordsService;
    @Autowired
    private ContactsMapper contactsMapper;
    @Autowired
    private ChatRecordsMapper chatRecordsMapper;
    @Value("${wechat.communet.notify.url}")
    private String url;

    //上传新增聊天记录
    @GetMapping("/upload")
    public ResultMsg ResultMsg(){
        int newMsgNums =  chatRecordsService.upload();
        return responseResultMsg(200, "success", newMsgNums + " new text messages uploaded");
    }

    //查看新增聊天记录
    @GetMapping("/all")
    public ResultMsg selectMessages(){
        List<Message> messageList = chatRecordsService.selectMessages();
        if(null == messageList || messageList.size() == 0) return responseResultMsg(200, "success", "没有更多新消息。");
        JSONArray json = JSONArray.parseArray((JSON.toJSONString(messageList)));
        return responseResultMsg(200, "success", json);
    }

    //查看新增聊天记录（测试版本dto）
    @GetMapping("/test")
    public ResultMsg selectMsgs(){
        List<Msg> msgList = chatRecordsService.selectTestMsg();
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

}
