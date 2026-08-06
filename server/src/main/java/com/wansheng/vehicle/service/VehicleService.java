package com.wansheng.vehicle.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wansheng.vehicle.dto.*;
import com.wansheng.vehicle.entity.InsuranceHistory;
import com.wansheng.vehicle.entity.InspectionHistory;
import com.wansheng.vehicle.entity.Vehicle;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * 车辆管理 Service
 */
public interface VehicleService {

    /**
     * 分页查询车辆列表
     */
    Page<Vehicle> listVehicles(String keyword, Integer vehicleType, int page, int size);

    /**
     * 获取车辆详情（含保险/年检历史）
     */
    Vehicle getDetail(Integer id);

    /**
     * 新增车辆
     */
    Vehicle create(VehicleDTO dto);

    /**
     * 批量新增车辆（JSON 数组）
     */
    int batchCreate(List<VehicleDTO> dtos);

    /**
     * Excel 批量导入车辆
     */
    int importFromExcel(MultipartFile file) throws IOException;

    /**
     * 更新车辆
     */
    Vehicle update(Integer id, VehicleDTO dto);

    /**
     * 删除车辆（软删除）
     */
    void delete(Integer id);

    /**
     * 续保 — 旧保险归档，写入新保险
     */
    InsuranceHistory renewInsurance(Integer vehicleId, RenewInsuranceDTO dto, MultipartFile attachment);

    /**
     * 更新年检
     */
    InspectionHistory updateInspection(Integer vehicleId, LocalDate inspectionDate, LocalDate expireDate, MultipartFile attachment);

    /**
     * 工作台仪表盘统计
     */
    DashboardStats getDashboardStats();

    /**
     * 获取即将到期/已逾期的车辆列表（工作台待办）
     */
    List<Vehicle> getExpiringVehicles();
}
