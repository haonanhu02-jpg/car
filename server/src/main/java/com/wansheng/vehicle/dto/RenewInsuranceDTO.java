package com.wansheng.vehicle.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 续保请求
 */
@Data
public class RenewInsuranceDTO {

    private String insuranceCompany;

    private String insuranceType;

    private String policyNumber;

    private java.time.LocalDate insuranceExpire;

    private BigDecimal premium;
}
