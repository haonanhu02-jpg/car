package com.wansheng.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保险历史记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("insurance_history")
public class InsuranceHistory {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联车辆ID */
    private Integer vehicleId;

    /** 保险公司 */
    private String insuranceCompany;

    /** 保单号 */
    private String policyNumber;

    /** 险种 */
    private String insuranceType;

    /** 保单截止日期 */
    private LocalDate insuranceExpire;

    /** 保费金额 */
    private BigDecimal premium;

    /** 保单附件路径 */
    private String attachmentUrl;

    /** 1-当前有效, 0-历史记录 */
    @Builder.Default
    private Integer isCurrent = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
