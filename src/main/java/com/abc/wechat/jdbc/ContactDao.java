package com.abc.wechat.jdbc;

import com.abc.wechat.utils.DBUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ContactDao {
    private static DBUtil dbUtil_;
    @Autowired
    public void setDbUtil_(DBUtil dbUtil){
        dbUtil_ = dbUtil;
    }

    public static Map<String, String> selectContacts(){
        Connection connection = dbUtil_.ContactConnection();
        Map<String, String> map = new HashMap<>();
        try {
            Statement stmt = connection.createStatement();
            String sql = "SELECT `UserName`,`NickName` FROM `Contact`;";
            ResultSet resultSet = stmt.executeQuery(sql);
            while(resultSet.next()){
                map.put(resultSet.getString(1), resultSet.getString(2));
            }
        }catch (SQLException e){
            log.error("SQL Expection");
        }
        return map;
    }
}
