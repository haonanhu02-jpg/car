package com.wansheng.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 提醒规则配置表
 *
 * 🎯 按提醒类型 + 提前天数维护提醒节点，以及提醒方式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reminder_config")
public class ReminderConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 提醒类型：0-保险, 1-年检 */
    private Integer type;

    /** 提前天数(30/15/7/3) */
    private Integer nodeDays;

    /** 是否启用：1-启用, 0-禁用 */
    @Builder.Default
    private Integer enabled = 1;

    /** 提醒方式：system/sms/email 的组合，逗号分隔 */
    private String remindMethods;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
