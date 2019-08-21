package com.abc.wechat.jdbc;

import com.abc.wechat.utils.DBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.*;

@Component
public class DeltaTimeDao {
    @Autowired
    private DBUtil dbUtil;
    public int selectUnixTimeStamp(){
        int unixTimeStamp = 0;
        try{
            Connection connection = dbUtil.DeltaTimeConnection();
            Statement statement = connection.createStatement();
            String sql = "SELECT `unixTimeStamp` FROM `base` WHERE id = '1'";
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) unixTimeStamp = resultSet.getInt(1);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return unixTimeStamp;
    }

    public void updateUnixTimeStamp(int unixTimeStamp){
        String sql = "UPDATE `base` SET `unixTimeStamp` = ? WHERE id = '1';";
        try {
            Connection connection = dbUtil.DeltaTimeConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, unixTimeStamp);
            preparedStatement.executeUpdate();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
}
