package com.abc.wechat.dao.db1;

import com.abc.wechat.entity.ChatMsg;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ChatRecordsMapper {
    @Options(useCache = false)
    List<ChatMsg> selectAllMsg();

    //select StrContent from MSG where localId = 3
    @Update("update `MSG` set `Reserved0` = #{random} where localId = 1")
    @Options(useCache = false)
    void testCache(int random);

}
