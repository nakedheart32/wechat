package com.abc.wechat.service.Impl;

import com.abc.wechat.dto.Message;
import com.abc.wechat.entity.ChatMsg;
import com.abc.wechat.entity.ExecuteBatchLog;
import com.abc.wechat.entity.ExecuteRecordLog;
import com.abc.wechat.jdbc.DeltaTimeDao;
import com.abc.wechat.jdbc.ExecuteLogDao;
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
    @Autowired
    private ExecuteLogDao executeLogDao;


    /**
     * 上传增量消息记录到MongoDB
     * @return 存入新消息的数目 res[0]成功   res[1]失败
     */
    public List<Integer> upload() {
        //查询原始记录
        long startTimeMillis = System.currentTimeMillis();
        List<ChatMsg> chatMsgList = msgDao.selectChatMsg(executeLogDao.getLastRecordTimestamp(), Constant.getGroups());
        List<Integer> res = new ArrayList<>();
        res.add(0);
        res.add(0);
        if(chatMsgList.size() == 0) return res;
        ExecuteBatchLog executeBatchLog = new ExecuteBatchLog();
        executeBatchLog.setFirstRecordTimestamp(Long.parseLong(chatMsgList.get(0).getCreateTime()));
        executeBatchLog.setLastRecordTimestamp(Long.parseLong(chatMsgList.get(chatMsgList.size() - 1).getCreateTime()));
        executeBatchLog.setId(UUID.randomUUID().toString());
        executeBatchLog.setTotalCount(chatMsgList.size());
        executeBatchLog.setExecuteTimestamp(startTimeMillis);



        //转换&上传。
        for(ChatMsg chatMsg : chatMsgList){
            //创建单条记录日志对象
            ExecuteRecordLog executeRecordLog = new ExecuteRecordLog();
            executeRecordLog.setId(chatMsg.getId().toString());
            executeRecordLog.setBatchId(executeBatchLog.getId());
            executeRecordLog.setExecuteTimestamp(Long.parseLong(chatMsg.getCreateTime()));
            executeRecordLog.setRecordTimestamp(executeBatchLog.getExecuteTimestamp());
            Message message = msgToMessage(chatMsg, executeRecordLog);
            if(null == message){
                executeBatchLog.addFailCount();
                executeRecordLog.setExecuteResult("0");
            }else if(doPost(message)){
                executeBatchLog.addSuccessCount();
                executeRecordLog.setExecuteResult("1");
            }else{
                executeBatchLog.addFailCount();
                executeRecordLog.setExecuteResult("0");
                executeRecordLog.setFailReason("上传MongoDB失败");
            }
            executeLogDao.instrt(executeRecordLog);

        }
        int newTimeStamp = Integer.parseInt(chatMsgList.get(chatMsgList.size() - 1).getCreateTime());
        deltaTimeDao.updateUnixTimeStamp(newTimeStamp);
        //存储本次日志
        long endTimeMillis = System.currentTimeMillis();

        executeBatchLog.setExecutionTakeTime(endTimeMillis - startTimeMillis);
        executeLogDao.insert(executeBatchLog);
        res.set(0, executeBatchLog.getSuccessCount());
        res.set(1, executeBatchLog.getSuccessCount());

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

    /***
     *  msg --Trensfer to--->> message
     * @param chatMsg：数据库聊天记录原始实体
     * @return message:待上传的
     */
    private Message msgToMessage(ChatMsg chatMsg, ExecuteRecordLog executeRecordLog){

        Message msg = new Message();
        msg.setType(Constant.type(chatMsg.getType())); //根据原始记录原始记录中的type编号映射到具体的类型
        msg.setRecv_time(StrUtils.createTime(chatMsg.getCreateTime()));
        msg.setRecv_timestamp(msg.getRecv_time().getTime());
        msg.setGname(Constant.getNameByWxid(chatMsg.getSender()) == null ? "unknown" : Constant.getNameByWxid(chatMsg.getSender())); //消息来源群名
        msg.setGid(chatMsg.getSender());  //消息来源群id
        msg.setSend_user(Constant.getNameByWxid(chatMsg.getSender()) == null ? "unkonwn" : Constant.getNameByWxid(chatMsg.getSender()));//发送者昵称
        msg.setSend_username( StrUtils.wxId(chatMsg.getExtra())); //发送者wxid
        msg.setRid(UUID.randomUUID());
        msg.setHeadImgUlr("");
        msg.setId(UUID.randomUUID());
        msg.setUid("80114481815754212");
        //单独判断1类型中是否有链接出现
        if(null != strUtils.getLink(chatMsg.getContent()) && "" != strUtils.getLink(chatMsg.getContent())){
            msg.setType(Constant.LINK);
        }
        //detail
        JSONObject content = new JSONObject();
        switch (msg.getType()){
            case Constant.TEXT:
                content.put("content", chatMsg.getContent());
                break;
            case Constant.LINK:
                content.put("thumb", "");
                content.put("title", "");
                content.put("url", strUtils.getLink(chatMsg.getContent()));
                content.put("content", "");
                break;
            case Constant.IMAGE:
                //调用Oss上传
                String imagePath = strUtils.imageFilePath(chatMsg.getExtra(), 0);//index 0:big image、index 1:thumb image
                if(null == imagePath || "" == imagePath) {
                    log.error("localId:{}空路径。", chatMsg.getId());
                    executeRecordLog.setFailReason("空路径");
                    return null;
                }
                File image = new File(imagePath);
                if(!image.exists()) {
                    String thumbImagePath = strUtils.imageFilePath(chatMsg.getExtra(),1);
                    image = new File(thumbImagePath);
                }
                String imageId= UUID.randomUUID().toString()+ strUtils.getExtName(image.getName(),'.');
                try {
                    ossService.uploadFile("10001", imageId, new FileInputStream(image));
                } catch (FileNotFoundException e) {
                    log.error("FileNotFound:{}", image);
                    executeRecordLog.setFailReason("图片不存在：" + imagePath);
                    return null;
                }
                String imageOssUrl = ossService.fileUrl("10001", imageId);
                System.out.println("oss url = " + imageOssUrl);//TODO
                content.put("thumb_url", "");
                content.put("big_url", imageOssUrl);
                break;
            case Constant.FILE:
                String filePath = strUtils.filePath(chatMsg.getExtra());//filePath指原始记录中提取的路径
                if(null == filePath || "" == filePath) {
                    log.error("localId:{}空路径。", chatMsg.getId());
                    executeRecordLog.setFailReason("空路径:");
                    return null;//可能提取不到路径
                }
                File sourceFile = new File(filePath);
                String fileId = UUID.randomUUID().toString()+ strUtils.getExtName(sourceFile.getName(),'.');
                try {
                    ossService.uploadFile("10001", fileId, new FileInputStream(sourceFile));
                } catch (IOException e) {
                    log.error("FileNotFound:{}", sourceFile);//可能路径上并不存在实际的文件
                    executeRecordLog.setFailReason("文件不存在");
                    return null;
                }
                String url = ossService.fileUrl("10001", fileId);
                content.put("file_url", url);
                content.put("filename", sourceFile.getName());
                content.put("file_type", strUtils.getExtName(sourceFile.getName(), '.'));
                content.put("filesize", sourceFile.length());
                content.put("content", "");
                break;

        }
        msg.setDetail(content.toJSONString());
        return msg;
    }




/*    @Override
    public List<Message> selectMessages() {
        //List<ChatMsg> chatMsgList = msgDao.selectChatMsg(deltaTimeDao.selectUnixTimeStamp(), Constant.getGroups());
            Message msg = new Message();
            msg.setType(Constant.type(chatMsg.getType())); //根据原始记录原始记录中的type编号映射到具体的类型
            msg.setRecv_time(StrUtils.createTime(chatMsg.getCreateTime()));
            msg.setRecv_timestamp(msg.getRecv_time().getTime());
            msg.setGname(Constant.getNameByWxid(chatMsg.getSender()) == null ? "unknown" : Constant.getNameByWxid(chatMsg.getSender())); //消息来源群名
            msg.setGid(chatMsg.getSender());  //消息来源群id
            msg.setSend_user(Constant.getNameByWxid(chatMsg.getSender()) == null ? "unkonwn" : Constant.getNameByWxid(chatMsg.getSender()));//发送者昵称
            msg.setSend_username( StrUtils.wxId(chatMsg.getExtra())); //发送者wxid
            msg.setRid(UUID.randomUUID());
            msg.setHeadImgUlr("");
            msg.setId(UUID.randomUUID());
            msg.setUid("80114481815754212");
            //单独判断1类型中是否有链接出现
            if(null != strUtils.getLink(chatMsg.getContent()) && "" != strUtils.getLink(chatMsg.getContent())){
                msg.setType(Constant.LINK);
            }
            //detail
            JSONObject content = new JSONObject();
            switch (msg.getType()){
                case Constant.TEXT:
                    content.put("content", chatMsg.getContent());
                    break;
                case Constant.LINK:
                    content.put("thumb", "");
                    content.put("title", "");
                    content.put("url", strUtils.getLink(chatMsg.getContent()));
                    content.put("content", "");
                    break;
                case Constant.IMAGE:
                    //调用Oss上传
                    String imagePath = strUtils.imageFilePath(chatMsg.getExtra(), 0);//index 0:big image、index 1:thumb image
                    if(null == imagePath || "" == imagePath) {
                        log.error("localId:{}无法获取图片文件,请核查。", chatMsg.getId());
                        return null;
                    }
                    File image = new File(imagePath);
                    if(!image.exists()) {
                        String thumbImagePath = strUtils.imageFilePath(chatMsg.getExtra(),1);
                        image = new File(thumbImagePath);
                    }
                    String imageId= UUID.randomUUID().toString()+ strUtils.getExtName(image.getName(),'.');
                    try {
                        ossService.uploadFile("10001", imageId, new FileInputStream(image));
                    } catch (FileNotFoundException e) {
                        log.error("FileNotFound:{}", image);
                        return null;
                    }
                    String imageOssUrl = ossService.fileUrl("10001", imageId);
                    System.out.println("oss url = " + imageOssUrl);//TODO
                    content.put("thumb_url", "");
                    content.put("big_url", imageOssUrl);
                    break;
                case Constant.FILE:
                    String filePath = strUtils.filePath(chatMsg.getExtra());//filePath指原始记录中提取的路径
                    if(null == filePath || "" == filePath) return null;//可能提取不到路径
                    File sourceFile = new File(filePath);
                    String fileId = UUID.randomUUID().toString()+ strUtils.getExtName(sourceFile.getName(),'.');
                    try {
                        ossService.uploadFile("10001", fileId, new FileInputStream(sourceFile));
                    } catch (IOException e) {
                        log.error("FileNotFound:{}", sourceFile);//可能路径上并不存在实际的文件
                        return null;
                    }
                    String url = ossService.fileUrl("10001", fileId);
                    content.put("file_url", url);
                    content.put("filename", sourceFile.getName());
                    content.put("file_type", strUtils.getExtName(sourceFile.getName(), '.'));
                    content.put("filesize", sourceFile.length());
                    content.put("content", "");
                    break;
            msg.setDetail(content.toJSONString());
        }
        //更新时间戳
        if(chatMsgList.size() == 0) return messageList;
        int newTimeStamp = Integer.parseInt(chatMsgList.get(chatMsgList.size() - 1).getCreateTime());
        deltaTimeDao.updateUnixTimeStamp(newTimeStamp);
        return messageList;
    }*/

}
