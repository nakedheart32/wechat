package com.abc.wechat.entity;

import lombok.Data;

@Data
public class ExecuteRecordLog {
    private String id;
    private String batchId;
    private String executeResult;
    private Long recordTimestamp;
    private Long executeTimestamp;
    private String failReason;

}
