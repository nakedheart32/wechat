package com.abc.wechat.jdbc;

import com.abc.wechat.dto.Message;
import com.abc.wechat.dto.Msg;
import com.abc.wechat.entity.ChatMsg;
import com.abc.wechat.service.OssService;
import com.abc.wechat.utils.DBUtil;
import com.abc.wechat.utils.StrUtils;
import com.alibaba.druid.support.spring.stat.annotation.Stat;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.omg.CORBA.PRIVATE_MEMBER;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.object.SqlCall;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class MsgDao {

    @Autowired
    private DBUtil dbUtil;
    @Autowired
    private DeltaTimeDao deltaTimeDao;
    @Autowired
    private OssService ossService;



    public  List<ChatMsg> selectChatMsg(){
        List<ChatMsg> chatMsgList = new ArrayList<>();
        int unixTimeStamp = deltaTimeDao.selectUnixTimeStamp();
        Connection connection = dbUtil.MessageConnection();
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            String sql = "SELECT  `localId`, `Type`, `IsSender`, `CreateTime`, `StrTalker`, `StrContent`, `BytesExtra` FROM `MSG` WHERE `CreateTime` > " + unixTimeStamp + ";";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                //
                Integer id = resultSet.getInt("localId");
                Integer type = resultSet.getInt("Type");
                Integer isSender = resultSet.getInt("IsSender");
                String createTime = resultSet.getInt("CreateTime") + "";
                String sender = resultSet.getString("StrTalker");
                String content = resultSet.getString("StrContent");
                String extra = resultSet.getString("BytesExtra");
                //System.out.println(extra);
                ChatMsg chatMsg = new ChatMsg();
                chatMsg.setId(id);
                chatMsg.setType(type);
                chatMsg.setIsSender(isSender);
                chatMsg.setSender(sender);
                chatMsg.setCreateTime(createTime);
                chatMsg.setContent(content);
                chatMsg.setExtra(extra);

                chatMsgList.add(chatMsg);

            }
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            try {
                if(null != resultSet) resultSet.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
            try{
                if(null != statement) statement.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
            try {
                if(null != connection) connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
        if(null == chatMsgList || 0 == chatMsgList.size()) return chatMsgList;
        unixTimeStamp = Integer.parseInt(chatMsgList.get(chatMsgList.size() - 1).getCreateTime());
        deltaTimeDao.updateUnixTimeStamp(unixTimeStamp);
        return chatMsgList;
    }

    //自测包装类
    public  List<Msg> selectMsg(){
        List<Msg> msgList = new ArrayList<>();
        List<ChatMsg> chatMsgList = selectChatMsg();
        if(chatMsgList.size() == 0) return msgList;

        Connection connection = dbUtil.ContactConnection();
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        try {
            String sql = "SELECT `NickName` FROM `Contact` WHERE `UserName` = ?";
            ps = connection.prepareStatement(sql);
            for(ChatMsg chatMsg : chatMsgList){
                Msg msg = new Msg();
                msg.setId(chatMsg.getId());
                //msg.setTime(StrUtils.createTime(chatMsg.getCreateTime()));
                msg.setContent(chatMsg.getContent());
                msg.setType(StrUtils.type(chatMsg.getType()));
                if(chatMsg.getIsSender() == 1) {
                    msg.setFrom("我");
                    msg.setGroup("非群聊");
                }else{
                    if(!StrUtils.isGroupChat(chatMsg.getSender())) {
                        ps.setString(1, chatMsg.getSender());
                        resultSet = ps.executeQuery();
                        msg.setFrom(resultSet.getString(1));
                        //msg.setFrom(contactsMapper.selectNameById(chatMsg.getSender()));
                        msg.setGroup("");
                    }else{
                        ps.setString(1, StrUtils.wxId(chatMsg.getExtra()));
                        resultSet = ps.executeQuery();
                        msg.setFrom(resultSet.getString(1));
                        //msg.setFrom(contactsMapper.selectNameById(StrUtils.wxId(chatMsg.getExtra())));

                        ps.setString(1, chatMsg.getSender());
                        resultSet = ps.executeQuery();
                        msg.setGroup(resultSet.getString(1));
                        //msg.setGroup(contactsMapper.selectNameById(chatMsg.getSender()));
                    }
                }
                msgList.add(msg);
            }
            if(null != resultSet) resultSet.close();
            ps.close();
            connection.close();
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            try {
                if(null != ps) ps.close();
            }catch (SQLException e){
                e.printStackTrace();
            }

            try {
                if(null != connection) connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
        return msgList;
    }

    //查詢包裝message（組成post的一部分）
    public  List<Message> selectMessage(){
        List<Message> messageList = new ArrayList<>();
        List<ChatMsg> chatMsgList = selectChatMsg();
        Connection connection = dbUtil.ContactConnection();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sql = "SELECT `NickName` FROM `Contact` WHERE `UserName` = ?";
            preparedStatement = connection.prepareStatement(sql);
            for(ChatMsg chatMsg : chatMsgList){
                Message msg = new Message();

                //type
                msg.setType(StrUtils.type(chatMsg.getType()));
                if(null != StrUtils.getLink(chatMsg.getContent()) && "" != StrUtils.getLink(chatMsg.getContent())){
                    msg.setType(StrUtils.LINK);
                }
                //detail
                JSONObject content = new JSONObject();
                if(StrUtils.TEXT == msg.getType())
                    content.put("content", chatMsg.getContent());
                else if(StrUtils.LINK == msg.getType()){
                    content.put("thumb", "");
                    content.put("title", "");
                    content.put("url", StrUtils.getLink(chatMsg.getContent()));
                    content.put("content", "");
                }else if(StrUtils.IMAGE == msg.getType()){
                    //调用Oss上传
                    File image = new File(StrUtils.imageFilePath(chatMsg.getExtra()));
                    String fileId= UUID.randomUUID().toString()+ StrUtils  .getExtName(image.getName(),'.');
                    try {
                        ossService.uploadFile("10001", fileId, new FileInputStream(image));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    String url = ossService.fileUrl("10001", fileId);
                    content.put("thumb_url", "");
                    content.put("big_url", url);
                }else if(StrUtils.FILE == msg.getType()){
                    String filePath = StrUtils.filePath(chatMsg.getExtra());
                    if(null == filePath) continue;
                    File sourceFile = new File(filePath);

                    String fileId= UUID.randomUUID().toString()+ StrUtils.getExtName(sourceFile.getName(),'.');
                    try {
                        ossService.uploadFile("10001", fileId, new FileInputStream(sourceFile));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String url = ossService.fileUrl("10001", fileId);
                    content.put("file_url", url);
                    content.put("filename", sourceFile.getName());
                    content.put("file_type", StrUtils.getExtName(sourceFile.getName(), '.'));
                    content.put("filesize", sourceFile.length());
                    content.put("content", "");
                }

                msg.setDetail(content.toJSONString());
                msg.setRecv_time(StrUtils.createTime(chatMsg.getCreateTime()));
                msg.setRecv_timestamp(msg.getRecv_time().getTime());

                //先判断群聊
                if(!StrUtils.isGroupChat(chatMsg.getSender())){
                    //非群消息
                    msg.setGname("非群聊");
                    msg.setGid("非群聊");
                    if(chatMsg.getIsSender() == 1){
                        msg.setSend_user("本地发出消息");
                        msg.setSend_username("本地发出消息");
                    }else{
                        preparedStatement.setString(1, chatMsg.getSender());
                        resultSet = preparedStatement.executeQuery();
                        msg.setSend_user(  resultSet.next() ? resultSet.getString(1) : "未知發送者");
                        msg.setSend_username(chatMsg.getSender());
                    }
                }else{
                    //群消息处理
                    //设置群名称和id
                    preparedStatement.setString(1, chatMsg.getSender());
                    resultSet = preparedStatement.executeQuery();
                    msg.setGname(resultSet.getString(1)); //消息来源群名
                    msg.setGid(chatMsg.getSender());  //消息来源群id

                    preparedStatement.setString(1, StrUtils.wxId(chatMsg.getExtra()));
                    resultSet = preparedStatement.executeQuery();
                    msg.setSend_user(resultSet.next() ?  resultSet.getString(1) : "UnknownSender");//发送者昵称
                    msg.setSend_username( StrUtils.wxId(chatMsg.getExtra())); //发送者id

                }
                msg.setRid(UUID.randomUUID());
                msg.setHeadImgUlr("");
                msg.setId(UUID.randomUUID());
                msg.setUid("80114481815754212");
                messageList.add(msg);

            }
            if(null != resultSet) resultSet.close();
            preparedStatement.close();
            connection.close();
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            try {
                if(null != resultSet) resultSet.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
            try {
                if(null != preparedStatement) preparedStatement.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
            try {
                if(null != connection) connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }

        return messageList;
    }

    //插入新消息到reupload表
    public int saveMessage(Message message){
        Connection connection = dbUtil.DeltaTimeConnection();
        PreparedStatement preparedStatement = null;
        int rows = 0;
        String sql = "INSERT INTO `reupload` VALUES(?, ?, ? , ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, message.getId().toString());
            preparedStatement.setString(2, message.getUid());
            preparedStatement.setString(3, message.getRid().toString());
            preparedStatement.setString(4, message.getSend_username());
            preparedStatement.setString(5, message.getSend_user());
            preparedStatement.setString(6,message.getGname());
            preparedStatement.setString(7, message.getGid());
            preparedStatement.setLong (8, message.getRecv_timestamp());
            preparedStatement.setString(9, message.getDetail());
            preparedStatement.setString(10, message.getType());
            preparedStatement.setDate(11, (Date) message.getRecv_time());
            preparedStatement.setString(12, message.getHeadImgUlr());
            rows = preparedStatement.executeUpdate(sql);

            preparedStatement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            try {
                if(null != preparedStatement) preparedStatement.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
            try {
                if(null != connection) connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
        return rows;

    }

    //查標誌位為0的reupload庫
    public List<Message> selectReuploadMessages() {
        Connection connection = dbUtil.DeltaTimeConnection();
        Statement statement = null;
        ResultSet resultSet = null;
        List<Message> messageList = new ArrayList<>();
        try {
            statement = connection.createStatement();
            String sql = "SELECT * FROM `reupload` WHERE `flag` = '0' ";
            resultSet = statement.executeQuery(sql);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd ");
            while (resultSet.next()){
                Message message = new Message();
                message.setHeadImgUlr(resultSet.getString("headImgUlr"));
                message.setUid(resultSet.getString("uid"));
                message.setSend_username(resultSet.getString("send_username"));
                message.setSend_user(resultSet.getString("send_user"));
                message.setGid(resultSet.getString("gid"));
                message.setGname(resultSet.getString("gname"));
                message.setType(resultSet.getString("type"));
                message.setDetail(resultSet.getString("detail"));
                message.setRecv_time(dateFormat.parse(resultSet.getString("recv_time")));
                message.setRecv_timestamp(resultSet.getLong("recv_timestamp"));
                message.setId(UUID.fromString(resultSet.getString("id")) );
                message.setRid(UUID.fromString(resultSet.getString("rid")));
                messageList.add(message);
            }
            resultSet.close();
            statement.close();
            connection.close();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(null != resultSet) resultSet.close();
            }catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if(null != statement) statement.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
            try {
                if(null != connection) connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }

        }
        return messageList;
    }

    public void deleteMessage(Message message) {
        Connection connection = dbUtil.DeltaTimeConnection();
        PreparedStatement preparedStatement = null;
        try {
            String sql = "UPDATE `reupload` SET `flag` = '1' WHERE uid = '?'";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, message.getUid());
            preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.close();
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            try {
                if(null != preparedStatement) preparedStatement.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
            try {
                if(null != connection) connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }


    }
}
