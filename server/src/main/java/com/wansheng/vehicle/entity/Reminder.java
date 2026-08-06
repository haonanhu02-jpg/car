package com.wansheng.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 到期提醒记录表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reminders")
public class Reminder {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联车辆ID */
    private Integer vehicleId;

    /** 提醒类型：0-保险, 1-年检 */
    private Integer type;

    /** 提前天数(30/15/7/3) */
    private Integer nodeDays;

    /** 实际提醒日期 */
    private LocalDate remindDate;

    /** 提醒方式：system/sms/email */
    private String remindMethod;

    /** 状态：0-待处理, 1-已处理, 2-已逾期 */
    @Builder.Default
    private Integer status = 0;

    /** 处理人 */
    private String handler;

    /** 处理时间 */
    private LocalDateTime handledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
