package com.abc.wechat.conroller;

import com.abc.wechat.dao.db1.ChatRecordsMapper;
import com.abc.wechat.dao.db2.ContactsMapper;
import com.abc.wechat.dto.Message;
import com.abc.wechat.dto.Msg;
import com.abc.wechat.entity.ChatMsg;
import com.abc.wechat.service.ChatRecordsService;
import com.abc.wechat.utils.HttpClientUtil;
import com.abc.wechat.utils.ResultMsg;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.DriverManager;
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
    @Value("${wechat.communet.notify.url}")
    private String url;


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

    @GetMapping("/jdbc")
    public ResultMsg jdbcTest(){
        List<ChatMsg> chatRecordList = chatRecordsService.selectRecords();
        JSONArray json = JSONArray.parseArray(JSON.toJSONString(chatRecordList));
        return responseResultMsg(200, "success", json);
    }

    @GetMapping("/msg")
    public ResultMsg selectMsgs(){
        List<Msg> msgList = chatRecordsService.selectMsgs();
        JSONArray json = JSONArray.parseArray((JSON.toJSONString(msgList)));
        return responseResultMsg(200, "success", json);
    }

    @GetMapping("/all")
    public ResultMsg selectMessages(){
        List<Message> messageList = chatRecordsService.selectMessages();
        if(null == messageList || messageList.size() == 0) return responseResultMsg(200, "success", "没有更多新消息。");
        JSONArray json = JSONArray.parseArray((JSON.toJSONString(messageList)));
        for(Message message : messageList){
            System.out.println("调用doPost");
            JSONObject jsonToPost = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            JSONObject messageJson = (JSONObject) JSON.toJSON(message);
            System.out.println(messageJson.toString());
            jsonArray.add(messageJson);
            jsonToPost.put("data", jsonArray);
            jsonToPost.put("type", 1);
            HttpClientUtil.doPostJson(url, jsonToPost.toString());
        }
        return responseResultMsg(200, "success", json);
    }



}
