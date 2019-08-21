package com.abc.wechat.service.Impl;

import com.abc.wechat.dao.db1.ChatRecordsMapper;
import com.abc.wechat.dao.db2.ContactsMapper;
import com.abc.wechat.dto.Message;
import com.abc.wechat.dto.Msg;
import com.abc.wechat.entity.ChatMsg;
import com.abc.wechat.service.ChatRecordsService;
import com.abc.wechat.utils.StrUtils;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChatRecordsServiceImpl implements ChatRecordsService {
    @Autowired
    private ChatRecordsMapper chatRecordsMapper;
    @Autowired
    private ContactsMapper contactsMapper;

    //增量查询参数
    private static  int unixTimeStamp;




    // mybatis -- DAO->charRecord
    @Override
    public List<ChatMsg> selectAllChatRecords() {
        return chatRecordsMapper.selectAllMsg();
    }

    // mybatis -- DTO->Msg
    @Override
    public List<Msg> selectAllMsgs(){
        List<Msg> msgList = new ArrayList<>();
        List<ChatMsg> chatMsgList = selectAllChatRecords();
        for(ChatMsg chatMsg : chatMsgList){
            Msg msg = new Msg();
            msg.setId(chatMsg.getId());
            //msg.setTime(StrUtils.createTime(chatMsg.getCreateTime()));
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

    // jdbc -- DAO->chatRecord
    @Override
    public List<ChatMsg> selectRecords(){
        Connection connection;
        List<ChatMsg> chatRecordList = new ArrayList<>();
        int unixTimeStamp = getDeltaT();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:F:\\newFile\\deWeChat\\decodeWeChat\\x64\\Debug\\dec_MSG0.db");
            System.out.println("connected to database.");
            final Statement statement = connection.createStatement();
            String sql = "SELECT  `localId`, `Type`, `IsSender`, `CreateTime`, `StrTalker`, `StrContent`, `BytesExtra` FROM `MSG` WHERE `CreateTime` > " + unixTimeStamp + ";";
            System.out.println(sql);
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

                chatRecordList.add(chatMsg);

            }
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if(null == chatRecordList || 0 == chatRecordList.size()) return chatRecordList;
        unixTimeStamp = Integer.parseInt(chatRecordList.get(chatRecordList.size() - 1).getCreateTime());
        updateDeltaT(unixTimeStamp);
        return chatRecordList;
    }

    //jdbc -- DTO->Msg
    @Override
    public List<Msg> selectMsgs() {
        List<Msg> msgList = new ArrayList<>();
        List<ChatMsg> chatMsgList = selectRecords();
        if(chatMsgList.size() == 0) return msgList;
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:F:\\ollyDB\\deWeChat\\decodeWeChat\\x64\\Debug\\2nd\\dec_MicroMsg.db");
            System.out.println("connected to database.");
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

    @Override
    public List<Message> selectMessages() {
        List<Message> messageList = new ArrayList<>();
        List<ChatMsg> chatMsgList = selectRecords();
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:F:\\ollyDB\\deWeChat\\decodeWeChat\\x64\\Debug\\2nd\\dec_MicroMsg.db");
            System.out.println("connected to database.");
            String sql = "SELECT `NickName` FROM `Contact` WHERE `UserName` = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet resultSet;
            for(ChatMsg chatMsg : chatMsgList){
                Message msg = new Message();
                JSONObject content = new JSONObject();
                content.put("content", chatMsg.getContent());
                msg.setDetail(content.toJSONString());
                msg.setRecv_time(StrUtils.createTime(chatMsg.getCreateTime()));
                //msg.setType(StrUtils.type(chatMsg.getType()));
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
                    //msg.setSend_username( StrUtils.wxId(chatMsg.getExtra()));
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

    private int getDeltaT(){
        String sql = "SELECT `unixTimeStamp` FROM `base` WHERE id = '1'";
        int deltaT = 0;
        try{
            Connection connection = DriverManager.getConnection("jdbc:sqlite:F:\\newFile\\deWeChat\\decodeWeChat\\x64\\Debug\\DeltaTime.db");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) deltaT = resultSet.getInt(1);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return deltaT;
    }

    private void updateDeltaT(int latestT){
        String sql = "UPDATE `base` SET `unixTimeStamp` = ? WHERE id = '1';";
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:F:\\newFile\\deWeChat\\decodeWeChat\\x64\\Debug\\DeltaTime.db");
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, latestT);
            preparedStatement.executeUpdate();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

}
