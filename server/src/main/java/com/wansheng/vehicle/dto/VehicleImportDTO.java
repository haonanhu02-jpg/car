package com.wansheng.vehicle.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * Excel 批量导入车辆 — 表头映射
 *
 * 列顺序无关，EasyExcel 按 @ExcelProperty 名称匹配。
 * 车辆类型：0-小车，1-大巴；Excel 中如无该列，导入时默认按 0（小车）处理。
 */
@Data
public class VehicleImportDTO {

    @ExcelProperty("车牌号")
    private String plateNumber;

    @ExcelProperty("车辆类型")
    private Integer vehicleType;

    @ExcelProperty("车辆品牌")
    private String brand;

    @DateTimeFormat("yyyy-MM-dd")
    @ExcelProperty("上牌时间")
    private LocalDate purchaseDate;

    @ExcelProperty("所属公司")
    private String owner;

    @ExcelProperty("投保公司")
    private String insuranceCompany;

    @ExcelProperty("险种")
    private String insuranceType;

    @ExcelProperty("保单号")
    private String policyNumber;

    @DateTimeFormat("yyyy-MM-dd")
    @ExcelProperty("保险截止")
    private LocalDate insuranceExpire;

    @DateTimeFormat("yyyy-MM-dd")
    @ExcelProperty("年检日期")
    private LocalDate inspectionExpire;

    @ExcelProperty("ETC办理")
    private String etcBank;

    @ExcelProperty("油卡号码")
    private String oilCardNumber;

    @ExcelProperty("备忘录")
    private String remark;
}
