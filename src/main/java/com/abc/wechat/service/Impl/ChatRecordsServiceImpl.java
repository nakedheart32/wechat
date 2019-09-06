package com.abc.wechat.service.Impl;

import com.abc.wechat.dao.db1.ChatRecordsMapper;
import com.abc.wechat.dao.db2.ContactsMapper;
import com.abc.wechat.dto.Message;
import com.abc.wechat.dto.Msg;
import com.abc.wechat.entity.ChatMsg;
import com.abc.wechat.jdbc.MsgDao;
import com.abc.wechat.service.ChatRecordsService;
import com.abc.wechat.utils.Constant;
import com.abc.wechat.utils.HttpClientUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatRecordsServiceImpl implements ChatRecordsService {
    @Autowired
    private ChatRecordsMapper chatRecordsMapper;
    @Autowired
    private ContactsMapper contactsMapper;
    @Autowired
    private MsgDao msgDao;
    @Value("${wechat.communet.notify.url}")
    private String url;
    //增量查询参数
    private static  int unixTimeStamp;


    // 查找聊天记录实体
    @Override
    public List<ChatMsg> selectChatMsg(){
        return msgDao.selectChatMsg();
    }

    // 查找聊天记录包装类 -> 测试用
    @Override
    public List<Msg> selectTestMsg() {
       return msgDao.selectMsg();
    }

    @Override
    public List<Message> selectMessages() {
        return  msgDao.selectMessage();
    }



    /**
     * 上传增量消息记录到MongoDB
     * @return 存入新消息的数目
     */
    @Override
    public List<Integer> upload() {
        List<Message> messageList = selectMessages();
        List<Integer> res = new ArrayList<>();
        res.add(0);res.add(0);
        if(null == messageList || messageList.size() == 0) return res;
        int cnt = 0;
        for(Message message : messageList)
            if(doPost(message)) ++cnt;
            else{
                msgDao.saveMessage(message);
            }

        res.set(0, cnt);
        res.set(1, messageList.size() - cnt);
        return res;
    }

    //从备份库查，dopost
    public List<Integer> reupload(){
        List<Message> messageList = selectReuploadMessages();
        List<Integer> res = new ArrayList<>();
        res.add(0); res.add(0);
        if(null == messageList || messageList.isEmpty()) return res;
        int cnt = 0;
        for(Message message : messageList){
            if(doPost(message)){
                ++cnt;
                msgDao.deleteMessage(message);
            }
        }
        res.set(0, cnt);
        res.set(1, messageList.size() - cnt);
        return res;
    }

    private List<Message> selectReuploadMessages() {
        return msgDao.selectReuploadMessages();
    }



    private boolean doPost(Message message){
        JSONObject jsonToPost = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONObject messageJson = (JSONObject) JSON.toJSON(message);
        jsonArray.add(messageJson);
        jsonToPost.put("data", jsonArray);
        jsonToPost.put("type", 1);

        String resultStr = HttpClientUtil.doPostJson(url, jsonToPost.toString());
        JSONObject resJson = JSON.parseObject(resultStr);
        if(0 == (int)resJson.get("code") ) return true;
        return false;
    }

}
