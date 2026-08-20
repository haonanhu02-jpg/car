package com.wansheng.vehicle.controller;

import com.wansheng.vehicle.dto.ApiResponse;
import com.wansheng.vehicle.dto.DashboardStats;
import com.wansheng.vehicle.entity.Vehicle;
import com.wansheng.vehicle.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工作台 API
 */
@Tag(name = "工作台", description = "首页仪表盘统计与待办列表")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final VehicleService vehicleService;

    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/statistics")
    public ApiResponse<DashboardStats> statistics() {
        return ApiResponse.success(vehicleService.getDashboardStats());
    }

    @Operation(summary = "获取即将到期/已逾期车辆风险列表")
    @GetMapping("/expiring")
    public ApiResponse<List<Vehicle>> expiringVehicles() {
        return ApiResponse.success(vehicleService.getExpiringVehicles());
    }
}
