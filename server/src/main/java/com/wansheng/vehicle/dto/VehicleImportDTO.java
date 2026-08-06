package com.wansheng.vehicle.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * Excel 批量导入车辆 — 表头映射
 *
 * 列顺序无关，EasyExcel 按 @ExcelProperty 名称匹配。
 * 车辆类型：0-小车，1-大巴
 */
@Data
public class VehicleImportDTO {

    @ExcelProperty("车牌号")
    private String plateNumber;

    @ExcelProperty("车辆类型")
    private Integer vehicleType;

    @ExcelProperty("品牌")
    private String brand;

    @DateTimeFormat("yyyy-MM-dd")
    @ExcelProperty("上牌日期")
    private LocalDate purchaseDate;

    @ExcelProperty("所属")
    private String owner;

    @ExcelProperty("投保公司")
    private String insuranceCompany;

    @ExcelProperty("险种")
    private String insuranceType;

    @ExcelProperty("保单号")
    private String policyNumber;

    @DateTimeFormat("yyyy-MM-dd")
    @ExcelProperty("保险到期")
    private LocalDate insuranceExpire;

    @DateTimeFormat("yyyy-MM-dd")
    @ExcelProperty("年检到期")
    private LocalDate inspectionExpire;

    @ExcelProperty("ETC银行")
    private String etcBank;

    @ExcelProperty("油卡号")
    private String oilCardNumber;

    @ExcelProperty("备注")
    private String remark;
}
