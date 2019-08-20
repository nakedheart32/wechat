package com.abc.wechat.utils;

import com.abc.wechat.dao.db2.ContactsMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StrUtils {

    public static final String GSTIME="yyyy-MM-dd HH:mm:ss";
    public static final String CHATROOM_PATTERN = "\\d+@chatroom";

    public static final String ID_PATTERN = "[a-zA-Z0-9_]+";
    public static final String TEXT = "文本";
    public static final String IMAGE = "图片";
    public static final String OTHER = "文件（小程序/链接）";

    @Autowired
    private ContactsMapper contactsMapper;


    //时间戳转换
    public static String createTime(String createTime){
        SimpleDateFormat format = new SimpleDateFormat(GSTIME);
        return  format.format(new Date(Long.parseLong(createTime)*1000));
    }

    //判断消息是否群聊
    public static boolean isGroupChat(String sender){
        Matcher matcher = Pattern.compile(CHATROOM_PATTERN).matcher(sender);
        return matcher.find();
    }

    //匹配wxid
    public static String wxId(String content){
        Matcher matcher = Pattern.compile(ID_PATTERN).matcher(content);
        matcher.find();
        return matcher.group();

    }

    //转换类型代码
    public static String type(Integer type){
        String type_;
        switch (type){
            case 1:
                type_ = StrUtils.TEXT;
                break;
            case 3:
                type_ = StrUtils.IMAGE;
                break;
            case 49:
                type_ = StrUtils.OTHER;
                break;
            default:
                type_ = "未知";
        }
        return type_;
    }



}



