package com.abc.wechat.service.Impl;

import com.abc.wechat.dto.Message;
import com.abc.wechat.entity.ChatMsg;
import com.abc.wechat.jdbc.DeltaTimeDao;
import com.abc.wechat.jdbc.MsgDao;
import com.abc.wechat.service.ChatRecordsService;
import com.abc.wechat.service.OssService;
import com.abc.wechat.utils.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Service
@Slf4j
public class ChatRecordsServiceImpl implements ChatRecordsService {


    @Autowired
    private MsgDao msgDao;
    @Value("${wechat.communet.notify.url}")
    private String url;
    @Autowired
    private DBUtil dbUtil;
    @Autowired
    private DeltaTimeDao deltaTimeDao;
    @Autowired
    private OssService ossService;
    @Autowired
    private StrUtils strUtils;
    @Autowired
    private ImageRevert imageRevert;


    /**
     * 上传增量消息记录到MongoDB
     * @return 存入新消息的数目 res[0]成功   res[1]失败
     */
    public List<Integer> upload() {
        List<Message> messageList = selectMessages();
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>处理完成将上传至MongoDB，共{}条>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>", messageList.size());
        List<Integer> res = new ArrayList<>();
        res.add(0);res.add(0);
        if(null == messageList || 0 == messageList.size() ) return res;
        int cnt = 0;
        for(Message message : messageList)
            if(doPost(message)) ++cnt;
            else msgDao.saveMessage(message);
        res.set(0, cnt);
        res.set(1, messageList.size() - cnt);
        return res;
    }

    /***
     * 从备份库重传失败的消息到MongoDB
     * @return res[0] 成功的消息数 res[1]失败的消息数
     */
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

    /***
     * 发送httppost，上传message到MongoDB
     * @param message 按照格式包装的消息对象
     * @return 是否上传成功 true or false
     */
    //上传聊天记录到MongoDB
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

    @Override
    public List<Message> selectMessages() {
        List<Message> messageList = new ArrayList<>();
        List<ChatMsg> chatMsgList = msgDao.selectChatMsg(deltaTimeDao.selectUnixTimeStamp(), Constant.getGroups());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>提取到原始记录，共{}条>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>", chatMsgList.size());
        for(ChatMsg chatMsg : chatMsgList){
            Message msg = new Message();
            //type
            msg.setType(Constant.type(chatMsg.getType()));
            if(null != strUtils.getLink(chatMsg.getContent()) && "" != strUtils.getLink(chatMsg.getContent())){
                msg.setType(Constant.LINK);
            }
            //detail
            JSONObject content = new JSONObject();
            if(Constant.TEXT == msg.getType())
                content.put("content", chatMsg.getContent());
            else if(Constant.LINK == msg.getType()){
                content.put("thumb", "");
                content.put("title", "");
                content.put("url", strUtils.getLink(chatMsg.getContent()));
                content.put("content", "");
            }else if(Constant.IMAGE == msg.getType()){
                //调用Oss上传
                String imagePath = strUtils.imageFilePath(chatMsg.getExtra());
                if(null == imagePath || "" == imagePath) continue;
                File image = new File(imagePath);
                String fileId= UUID.randomUUID().toString()+ strUtils.getExtName(image.getName(),'.');
                try {
                    ossService.uploadFile("10001", fileId, new FileInputStream(image));
                } catch (FileNotFoundException e) {
                    log.error("FileNotFound:{}", image);
                    continue;
                }
                String url = ossService.fileUrl("10001", fileId);
                System.out.println("oss url = " + url);
                content.put("thumb_url", "");
                content.put("big_url", url);
            }else if(Constant.FILE == msg.getType()){
                String filePath = strUtils.filePath(chatMsg.getExtra());
                if(null == filePath || "" == filePath) continue;
                File sourceFile = new File(filePath);
                String fileId= UUID.randomUUID().toString()+ strUtils.getExtName(sourceFile.getName(),'.');
                try {
                    ossService.uploadFile("10001", fileId, new FileInputStream(sourceFile));
                } catch (IOException e) {
                    log.error("FileNotFound:{}", sourceFile);
                    continue;
                }
                String url = ossService.fileUrl("10001", fileId);
                content.put("file_url", url);
                content.put("filename", sourceFile.getName());
                content.put("file_type", strUtils.getExtName(sourceFile.getName(), '.'));
                content.put("filesize", sourceFile.length());
                content.put("content", "");
            }
            msg.setDetail(content.toJSONString());
            msg.setRecv_time(StrUtils.createTime(chatMsg.getCreateTime()));
            msg.setRecv_timestamp(msg.getRecv_time().getTime());
            msg.setGname(msgDao.selectNicknameByWxid(chatMsg.getSender())); //消息来源群名
            msg.setGid(chatMsg.getSender());  //消息来源群id
            msg.setSend_user(msgDao.selectNicknameByWxid(StrUtils.wxId(chatMsg.getExtra())));//发送者昵称
            msg.setSend_username( StrUtils.wxId(chatMsg.getExtra())); //发送者wxid
            msg.setRid(UUID.randomUUID());
            msg.setHeadImgUlr("");
            msg.setId(UUID.randomUUID());
            msg.setUid("80114481815754212");
            messageList.add(msg);
        }
        //更新时间戳
        if(chatMsgList.size() == 0) return messageList;
        int newTimeStamp = Integer.parseInt(chatMsgList.get(chatMsgList.size() - 1).getCreateTime());
        deltaTimeDao.updateUnixTimeStamp(newTimeStamp);
        return messageList;

    }

}
