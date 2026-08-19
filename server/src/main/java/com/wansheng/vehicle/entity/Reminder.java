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

    /** 本轮保险/年检截止日期，用于将多个提醒节点合并成一条待办 */
    private LocalDate expireDate;

    /** 提醒方式：system/sms/email */
    private String remindMethod;

    /** 状态：0-待处理, 1-已处理, 2-超时未处理 */
    @Builder.Default
    private Integer status = 0;

    /** 处理人 */
    private String handler;

    /** 处理时间 */
    private LocalDateTime handledAt;

    /** 是否已归档：0-否，1-是 */
    @Builder.Default
    private Integer archived = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ──────────────────────────────────────
    //  非数据库字段（用于前端展示）
    // ──────────────────────────────────────

    /** 车牌号（联查 vehicles 表得到） */
    @TableField(exist = false)
    private String plateNumber;

    /** 距离实际截止日的天数，负数表示已经过期 */
    @TableField(exist = false)
    private Long remainingDays;

    /** 实际到期情况展示文案 */
    @TableField(exist = false)
    private String expireStatus;
}
