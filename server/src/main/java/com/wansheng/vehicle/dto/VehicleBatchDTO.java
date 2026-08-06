package com.wansheng.vehicle.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量新增车辆请求包装
 *
 * 🎯 用包装类承载 List，确保 @Valid 能级联校验每个元素
 */
@Data
public class VehicleBatchDTO {

    @Valid
    private List<VehicleDTO> items = new ArrayList<>();
}
