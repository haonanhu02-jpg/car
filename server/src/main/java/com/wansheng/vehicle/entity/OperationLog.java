package com.wansheng.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 操作人ID */
    private Integer userId;

    /** 操作人姓名 */
    private String userName;

    /** 目标车辆ID */
    private Integer vehicleId;

    /** 操作类型：CREATE/UPDATE/DELETE/RENEW_INSURANCE/UPDATE_INSPECTION */
    private String action;

    /** 操作描述 */
    private String description;

    /** 变更前数据（JSON） */
    private String beforeData;

    /** 变更后数据（JSON） */
    private String afterData;

    /** 操作IP */
    private String ipAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
