package com.wansheng.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 车辆创建/更新请求 DTO
 */
@Data
public class VehicleDTO {

    @NotBlank(message = "车牌号不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5][A-Z]\\.[A-Z0-9]{5,6}$", message = "车牌号格式不正确（如：浙J.U0055）")
    private String plateNumber;

    @NotNull(message = "车辆类型不能为空")
    private Integer vehicleType;

    private String brand;

    private LocalDate purchaseDate;

    private String owner;

    private String insuranceCompany;

    private String insuranceType;

    private String policyNumber;

    private LocalDate insuranceExpire;

    private LocalDate inspectionExpire;

    private String etcBank;

    private String oilCardNumber;

    private String remark;
}
