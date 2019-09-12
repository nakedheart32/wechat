package com.abc.wechat.utils;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Data
public class StrUtils {

    private String baseTar = "F:\\\\run\\\\abc9793326235\\\\FileStorage\\\\Image\\\\";

    //时间戳转换
    public static Date createTime(String createTime){
        SimpleDateFormat format = new SimpleDateFormat(Constant.GSTIME);
        String date = format.format(new Date(Long.parseLong(createTime)*1000));
        Date recv_time = new Date();
        try {
            recv_time = new SimpleDateFormat(Constant.GSTIME).parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return recv_time;
    }


    //判断消息是否群聊
    public static boolean isGroupChat(String sender){
        Matcher matcher = Pattern.compile(Constant.CHATROOM_PATTERN).matcher(sender);
        return matcher.find();
    }

    //匹配wxid
    public static String wxId(String content){
        Matcher matcher = Pattern.compile(Constant.ID_PATTERN).matcher(content);
        matcher.find();
        return matcher.group();

    }

    //匹配link
    public String getLink(String StrContent){
        Matcher matcher = Pattern.compile(Constant.URL_PATTERN).matcher(StrContent);
        if(!matcher.find()) return "";
        return matcher.group();
    }


    public static String getExtName(String s, char split) {
        int i = s.indexOf(split);
        int leg = s.length();
        return (i > 0 ? (i + 1) == leg ? " " : s.substring(i, s.length()) : " ");
    }


    //取解码后的图片地址。若没有返回空串""
    public String imageFilePath(String BytesExtra){
        Matcher m = Pattern.compile(Constant.IMAGE_PATTERN).matcher(BytesExtra);
        String imagePath = "";
        if(m.find()) {
            imagePath = m.group();
            imagePath = imagePath.substring(6, imagePath.length() - 4) + ".jpg";
        }else return null;
        return baseTar + imagePath;
    }

    public String thisMonth(){
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
        return simpleDateFormat.format(date);
    }

    public String filePath(String BytesExtra){
        Matcher m = Pattern.compile(Constant.FILE_PATTERN).matcher(BytesExtra);
        if(m.find()) return m.group();
        return null;
    }
}



