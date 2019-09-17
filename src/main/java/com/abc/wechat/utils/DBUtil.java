package com.abc.wechat.utils;

import com.abc.wechat.dto.Message;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Data
@Component
public class DBUtil {
    private  String Message_URL;
    private  String Contact_URL;
    private  String DeltaTime_URL;
    private String driver = "jdbc:sqlite:";
    private String messageDB = "\\dec_MSG0.db";
    private String contactDB = "\\dec_MicroMsg.db";
    private String deltaTimeDB="\\DeltaTime.db";

    @Value("${workspace}")
    private String workspace;

    public  Connection MessageConnection(){
        if(null == Message_URL) Message_URL = new StringBuilder().append(driver).append(workspace).append(messageDB).toString();
        Connection connection;
        try {
            connection = DriverManager.getConnection(Message_URL);
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public  Connection ContactConnection(){
        if(null == Contact_URL) Contact_URL = new StringBuilder().append(driver).append(workspace).append(contactDB).toString();
        Connection connection;
        try {
            connection = DriverManager.getConnection(Contact_URL);
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public  Connection DeltaTimeConnection(){
        if(null == DeltaTime_URL) DeltaTime_URL = new StringBuilder().append(driver).append(workspace).append(deltaTimeDB).toString();
        Connection connection;
        try {
            connection = DriverManager.getConnection(DeltaTime_URL);
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
