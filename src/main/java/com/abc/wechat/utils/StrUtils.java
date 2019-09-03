package com.abc.wechat.utils;

import com.abc.wechat.dao.db2.ContactsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Component
public class StrUtils {

    public static final String GSTIME="yyyy-MM-dd";
    public static final String CHATROOM_PATTERN = "\\d+@chatroom";

    public static final String ID_PATTERN = "[a-zA-Z0-9_]+";
    public static final String URL_PATTERN = "([hH][tT]{2}[pP]://|[hH][tT]{2}[pP][sS]://)(([A-Za-z0-9-~]+).)+([A-Za-z0-9-~\\\\/])+";
    public static final String FILE_PATTERN = "F:\\\\wechatfiles\\\\WeChat Files\\\\abc9793326235\\\\FileStorage\\\\File.*";;
    public static final String IMAGE_PATTERN = "Image.*?dat";

    public static final String TEXT = "TEXT";
    public static final String IMAGE = "IMAGE";
    public static final String LINK = "LINK";
    public static final String FILE = "FILE";




    @Autowired
    private ContactsMapper contactsMapper;


    //时间戳转换
    public static Date createTime(String createTime){
        SimpleDateFormat format = new SimpleDateFormat(GSTIME);
        String date = format.format(new Date(Long.parseLong(createTime)*1000));
        Date recv_time = new Date();
        try {
            recv_time = new SimpleDateFormat(GSTIME).parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return recv_time;
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

    //匹配link
    public static String getLink(String StrContent){
        Matcher matcher = Pattern.compile(URL_PATTERN).matcher(StrContent);
        if(!matcher.find()) return "";
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
                type_ = StrUtils.FILE;
                break;
            default:
                type_ = "Unknown";
        }
        return type_;
    }

    public static String getExtName(String s, char split) {
        int i = s.indexOf(split);
        int leg = s.length();
        return (i > 0 ? (i + 1) == leg ? " " : s.substring(i, s.length()) : " ");
    }

    public static String filePath(String BytesExtra){
        Matcher m = Pattern.compile(FILE_PATTERN).matcher(BytesExtra);
        if(m.find()) return m.group();
        return null;
    }

    //取解码后的图片地址。若没有返回空串""
    public static String imageFilePath(String BytesExtra){
        Matcher m = Pattern.compile(IMAGE_PATTERN).matcher(BytesExtra);
        String imagePath = "";
        if(m.find()) imagePath = m.group();
        imagePath = "F:\\run\\" + imagePath + ".jpg";
        return imagePath;
    }

}



