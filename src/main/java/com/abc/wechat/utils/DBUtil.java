package com.abc.wechat.utils;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Data
@Component
public class DBUtil {
    @Value("${sqliteDB.message.url}")
    private  String Message_URL;

    @Value("${sqliteDB.contact.url}")
    private  String Contact_URL;

    @Value("${sqliteDB.deltaTime.url}")
    private  String DeltaTime_URL;


    public  Connection MessageConnection(){
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
