package com.abc.wechat.jdbc;


import com.abc.wechat.entity.ExecuteBatchLog;
import com.abc.wechat.entity.ExecuteRecordLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExecuteLogDao {

    /**
     * 获取最近一次执行任务的时间戳
     * @return 时间戳
     */
    @Select("SELECT execute_timestamp FROM ExecuteBatchLog order by execute_timestamp desc limit 1")
    Integer getLastBatchTimestamp();

    @Select("SELECT last_record_timestamp FROM ExecuteBatchLog order by execute_timestamp desc limit 1")
    Long getLastRecordTimestamp();

    /**
     * 插入批次执行日志
     * @param model
     * @return
     */
    @Insert("INSERT INTO ExecuteBatchLog(id,execute_Timestamp,total_count,success_count,fail_count,first_record_timestamp,last_record_timestamp,execution_take_time)" +
            " VALUES(#{id},#{executeTimestamp},#{totalCount},#{successCount},#{failCount},#{firstRecordTimestamp},#{lastRecordTimestamp},#{executionTakeTime})")
    int insert(ExecuteBatchLog model);

    @Insert("INSERT INTO ExecuteRecordLog(id, batch_id, execute_result, record_timestamp, execute_timestamp, fail_reason)" +
            " VALUES(#{id}, #{batchId}, #{executeResult}, #{recordTimestamp}, #{executeTimestamp}, #{failReason})")
    int instrt(ExecuteRecordLog model);

}
