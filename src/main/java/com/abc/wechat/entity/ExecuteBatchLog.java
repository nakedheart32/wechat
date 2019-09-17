package com.abc.wechat.entity;

import lombok.Data;

/**
 * @author: ylwang
 * @date: 2019/9/12 19:01
 * @description: 执行批次日志
 */
@Data
public class ExecuteBatchLog {
    private String id;
    private Long executeTimestamp;
    private Integer totalCount;
    private Integer successCount=0;
    private Integer failCount=0;
    private Long firstRecordTimestamp;
    private Long lastRecordTimestamp;
    private Long executionTakeTime;

    public Integer addSuccessCount(){
        return successCount++;
    }
    public Integer addFailCount(){
        return failCount++;
    }

}