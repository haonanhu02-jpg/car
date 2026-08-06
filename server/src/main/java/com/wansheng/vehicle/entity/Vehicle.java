package com.wansheng.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆台账主表
 *
 * 🎯 对应需求文档 vehicles 表，13个展示列
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("vehicles")
public class Vehicle {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 车牌号（唯一） */
    private String plateNumber;

    /** 车辆类型：0-小车, 1-大巴 */
    private Integer vehicleType;

    /** 车辆品牌 */
    private String brand;

    /** 上牌时间 */
    private LocalDate purchaseDate;

    /** 所属公司 */
    @Builder.Default
    private String company = "万盛股份";

    /** 产权所属 */
    private String owner;

    /** 投保公司 */
    private String insuranceCompany;

    /** 险种 */
    private String insuranceType;

    /** 保单号 */
    private String policyNumber;

    /** 保险截止日期 */
    private LocalDate insuranceExpire;

    /** 年检截止日期 */
    private LocalDate inspectionExpire;

    /** ETC办理银行 */
    private String etcBank;

    /** 油卡号码 */
    private String oilCardNumber;

    /** 备注 */
    private String remark;

    /** 状态：1-正常, 0-已注销 */
    @Builder.Default
    private Integer status = 1;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ──────────────────────────────────────
    //  非数据库字段（用于前端展示）
    // ──────────────────────────────────────

    @TableField(exist = false)
    private String statusLabel;

    /**
     * 计算车辆到期状态
     * 🟢 正常（>30天） 🟡 即将到期（≤30天） 🔴 已逾期
     */
    public String getStatusLabel() {
        LocalDate today = LocalDate.now();
        LocalDate earliest = getEarliestExpireDate();
        if (earliest == null) return "🟢 正常";

        long daysUntil = today.until(earliest).getDays();
        if (daysUntil < 0) return "🔴 已逾期";
        if (daysUntil <= 30) return "🟡 即将到期";
        return "🟢 正常";
    }

    /**
     * 获取最早的到期日期（保险 vs 年检）
     */
    public LocalDate getEarliestExpireDate() {
        if (insuranceExpire == null && inspectionExpire == null) return null;
        if (insuranceExpire == null) return inspectionExpire;
        if (inspectionExpire == null) return insuranceExpire;
        return insuranceExpire.isBefore(inspectionExpire) ? insuranceExpire : inspectionExpire;
    }
}
