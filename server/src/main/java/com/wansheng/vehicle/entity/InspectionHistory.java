package com.wansheng.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 年检历史记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("inspection_history")
public class InspectionHistory {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer vehicleId;

    /** 年检日期 */
    private LocalDate inspectionDate;

    /** 年检截止日期 */
    private LocalDate expireDate;

    /** 年检报告附件路径 */
    private String attachmentUrl;

    /** 1-当前有效, 0-历史记录 */
    @Builder.Default
    private Integer isCurrent = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
