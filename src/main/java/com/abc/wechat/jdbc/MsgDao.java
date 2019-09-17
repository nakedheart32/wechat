package com.abc.wechat.jdbc;

import com.abc.wechat.dto.Message;
import com.abc.wechat.entity.ChatMsg;
import com.abc.wechat.service.OssService;
import com.abc.wechat.utils.Constant;
import com.abc.wechat.utils.DBUtil;
import com.abc.wechat.utils.ImageRevert;
import com.abc.wechat.utils.StrUtils;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    @Autowired
    private StrUtils strUtils;
    @Autowired
    private ImageRevert imageRevert;

    //查原始消息，返回ChatMsg列表
    public List<ChatMsg> selectChatMsg(Long timestamp, String groups){

        String unixTimeStamp = "'" + timestamp + "'";
        groups = groups.replace(",", "','");
        groups = "('" + groups + "')";


        //解码图片
        imageRevert.revertImage();

        List<ChatMsg> chatMsgList = new ArrayList<>();
        Connection connection = dbUtil.MessageConnection();
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            String sql = "SELECT  `localId`, `Type`, `IsSender`, `CreateTime`, `StrTalker`, `StrContent`, `BytesExtra` FROM `MSG` WHERE `CreateTime` > "+
                    unixTimeStamp +
                    "AND (`Type` IN "+"(1, 3, 49)"+
                    " AND `StrTalker` in " + groups +");";
            System.out.println(sql);
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
        return chatMsgList;
    }

    //根据wxid查昵称
    public String selectNicknameByWxid(String wxid){
        Connection connection = dbUtil.ContactConnection();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String nickName = "";
        try {
            String sql = "SELECT `NickName` FROM `Contact` WHERE `UserName` = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, wxid);
            resultSet = statement.executeQuery();
            if(resultSet.next()) nickName = resultSet.getString(1);
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
        return nickName;
    }



    //插入新消息到reupload表
    public int saveMessage(Message message){
        Connection connection = dbUtil.DeltaTimeConnection();
        PreparedStatement preparedStatement = null;
        int rows = 0;
        String sql = "INSERT INTO `reupload` VALUES(?, ?, ? , ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            preparedStatement.setString(11, message.getRecv_time().toString());
            preparedStatement.setString(12, message.getHeadImgUlr());
            preparedStatement.setInt(13, 0);
            rows = preparedStatement.executeUpdate();

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

    //查標誌位為0的reupload表
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

    //删除成功补传后的记录
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
