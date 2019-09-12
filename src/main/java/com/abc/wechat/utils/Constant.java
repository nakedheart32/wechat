package com.abc.wechat.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Constant {
    public static final String GSTIME="yyyy-MM-dd";
    public static final String CHATROOM_PATTERN = "\\d+@chatroom";

    public static final String ID_PATTERN = "[a-zA-Z0-9_]+";
    public static final String URL_PATTERN = "([hH][tT]{2}[pP]://|[hH][tT]{2}[pP][sS]://)(([A-Za-z0-9-~]+).)+([A-Za-z0-9-~\\\\/])+";
    public static final String IMAGE_PATTERN = "Image.*?dat";
    public static final String FILE_PATTERN = "[C-H]:.*File\\\\.*\\..*(?<!gif)$";

    public static final String TEXT = "TEXT";
    public static final String IMAGE = "IMAGE";
    public static final String LINK = "LINK";
    public static final String FILE = "FILE";

    private static String groups;

    @Value("${groupList}")
    public  void setGroups(String groupStr) {
        System.out.println(groupStr);
        groups = groupStr;
    }
    public static String getGroups() {
        return groups;
    }

    public static String type(Integer type){
        String type_;
        switch (type){
            case 1:
                type_ = Constant.TEXT;
                break;
            case 3:
                type_ = Constant.IMAGE;
                break;
            case 49:
                type_ = Constant.FILE;
                break;
            default:
                type_ = "Unknown";
        }
        return type_;
    }
}
