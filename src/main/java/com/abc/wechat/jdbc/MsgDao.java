package com.abc.wechat.jdbc;

import com.abc.wechat.dto.Message;
import com.abc.wechat.dto.Msg;
import com.abc.wechat.entity.ChatMsg;
import com.abc.wechat.utils.DBUtil;
import com.abc.wechat.utils.StrUtils;
import com.alibaba.fastjson.JSONObject;
import org.omg.CORBA.PRIVATE_MEMBER;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class MsgDao {

    @Autowired
    private DBUtil dbUtil;
    @Autowired
    private DeltaTimeDao deltaTimeDao;

    public  List<ChatMsg> selectChatMsg(){
        List<ChatMsg> chatMsgList = new ArrayList<>();
        int unixTimeStamp = deltaTimeDao.selectUnixTimeStamp();
        try {
            Connection connection = dbUtil.MessageConnection();
            Statement statement = connection.createStatement();
            String sql = "SELECT  `localId`, `Type`, `IsSender`, `CreateTime`, `StrTalker`, `StrContent`, `BytesExtra` FROM `MSG` WHERE `CreateTime` > " + unixTimeStamp + ";";
            ResultSet resultSet = statement.executeQuery(sql);
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
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if(null == chatMsgList || 0 == chatMsgList.size()) return chatMsgList;
        unixTimeStamp = Integer.parseInt(chatMsgList.get(chatMsgList.size() - 1).getCreateTime());
        deltaTimeDao.updateUnixTimeStamp(unixTimeStamp);
        return chatMsgList;
    }
    public  List<Msg> selectMsg(){
        List<Msg> msgList = new ArrayList<>();
        List<ChatMsg> chatMsgList = selectChatMsg();
        if(chatMsgList.size() == 0) return msgList;
        try {
            Connection connection = dbUtil.ContactConnection();
            String sql = "SELECT `NickName` FROM `Contact` WHERE `UserName` = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
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
                        ResultSet resultSet = ps.executeQuery();
                        msg.setFrom(resultSet.getString(1));
                        //msg.setFrom(contactsMapper.selectNameById(chatMsg.getSender()));
                        msg.setGroup("");
                    }else{
                        ps.setString(1, StrUtils.wxId(chatMsg.getExtra()));
                        ResultSet resultSet = ps.executeQuery();
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
            ps.close();
            connection.close();
        }catch (SQLException e){
            e.printStackTrace();
        }
        return msgList;
    }
    public  List<Message> selectMessage(){
        List<Message> messageList = new ArrayList<>();
        List<ChatMsg> chatMsgList = selectChatMsg();
        Connection connection;
        try {
            connection = dbUtil.ContactConnection();
            String sql = "SELECT `NickName` FROM `Contact` WHERE `UserName` = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet resultSet;
            for(ChatMsg chatMsg : chatMsgList){
                Message msg = new Message();
                JSONObject content = new JSONObject();
                content.put("content", chatMsg.getContent());
                msg.setDetail(content.toJSONString());
                msg.setRecv_time(StrUtils.createTime(chatMsg.getCreateTime()));
                msg.setType("TEXT");
                msg.setRecv_timestamp(msg.getRecv_time().getTime());

                //先判断群聊
                if(!StrUtils.isGroupChat(chatMsg.getSender())){
                    msg.setGname("非群聊");
                    msg.setGid("非群聊");
                    if(chatMsg.getIsSender() == 1){
                        msg.setSend_user("本地发出消息");
                        msg.setSend_username("本地发出消息");
                    }else{
                        ps.setString(1, chatMsg.getSender());
                        resultSet = ps.executeQuery();
                        msg.setSend_user(resultSet.getString(1));
                        msg.setSend_username(chatMsg.getSender());
                    }
                }else{
                    ps.setString(1, chatMsg.getSender());
                    resultSet = ps.executeQuery();
                    msg.setGname(resultSet.getString(1));
                    msg.setGid(chatMsg.getSender());

                    ps.setString(1, StrUtils.wxId(chatMsg.getExtra()));
                    resultSet = ps.executeQuery();
                    msg.setSend_user(resultSet.getString(1));
                    msg.setSend_username( StrUtils.wxId(chatMsg.getExtra()));

                }
                msg.setRid(UUID.randomUUID());
                msg.setHeadImgUlr("");
                msg.setId(UUID.randomUUID());
                msg.setUid("80114481815754212");
                messageList.add(msg);

            }
            ps.close();
            connection.close();
        }catch (SQLException e){
            e.printStackTrace();
        }

        return messageList;
    }

    public int saveMessage(Message message){
        Connection connection = dbUtil.DeltaTimeConnection();
        int rows = 0;
        String sql = "INSERT INTO `reupload` VALUES(?, ?, ? , ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;

    }

    //查標誌位為0的reupload庫
    public List<Message> selectReuploadMessages() {
        Connection connection = dbUtil.DeltaTimeConnection();
        List<Message> messageList = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM `reupload` WHERE `flag` = '0' ";
            ResultSet resultSet = statement.executeQuery(sql);
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
        }catch (Exception e){
            e.printStackTrace();
        }
        return messageList;
    }

}
