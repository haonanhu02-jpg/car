package com.wansheng.vehicle.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wansheng.vehicle.dto.*;
import com.wansheng.vehicle.entity.*;
import com.wansheng.vehicle.repository.*;
import com.wansheng.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 车辆管理 Service 实现
 *
 * 🎯 业务逻辑核心层：
 *    - 负责业务规则校验
 *    - 编排多个 Repository 调用
 *    - 管理事务边界
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleMapper vehicleMapper;
    private final InsuranceHistoryMapper insuranceHistoryMapper;
    private final InspectionHistoryMapper inspectionHistoryMapper;
    private final ReminderMapper reminderMapper;
    private final OperationLogMapper operationLogMapper;

    // ──────────────────────────────────────
    //  查询
    // ──────────────────────────────────────

    @Override
    public Page<Vehicle> listVehicles(String keyword, Integer vehicleType, int page, int size) {
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                .like(Vehicle::getPlateNumber, keyword)
                .or()
                .like(Vehicle::getBrand, keyword)
            );
        }
        if (vehicleType != null) {
            wrapper.eq(Vehicle::getVehicleType, vehicleType);
        }

        wrapper.eq(Vehicle::getStatus, 1)  // 只查正常车辆
               .orderByDesc(Vehicle::getCreatedAt);

        return vehicleMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Vehicle getDetail(Integer id) {
        return vehicleMapper.selectById(id);
    }

    @Override
    public DashboardStats getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(30);

        long totalVehicles = vehicleMapper.selectCount(
            new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getStatus, 1)
        );
        long todayExpiring = vehicleMapper.findTodayExpiring(today).size();
        long expiringSoon = vehicleMapper.findExpiringSoon(today, deadline).size();
        long overdue = vehicleMapper.findOverdue(today).size();

        return DashboardStats.builder()
                .totalVehicles(totalVehicles)
                .todayExpiring(todayExpiring)
                .expiringSoon(expiringSoon)
                .overdue(overdue)
                .build();
    }

    @Override
    public List<Vehicle> getExpiringVehicles() {
        LocalDate today = LocalDate.now();
        List<Vehicle> all = new ArrayList<>();
        all.addAll(vehicleMapper.findOverdue(today));           // 已逾期 → 最紧急
        all.addAll(vehicleMapper.findTodayExpiring(today));     // 今日到期
        all.addAll(vehicleMapper.findExpiringSoon(today, today.plusDays(30))); // 即将到期
        return all;
    }

    // ──────────────────────────────────────
    //  命令（写操作）
    // ──────────────────────────────────────

    @Override
    @Transactional
    public Vehicle create(VehicleDTO dto) {
        Vehicle vehicle = new Vehicle();
        BeanUtils.copyProperties(dto, vehicle);
        vehicle.setStatus(1);
        vehicleMapper.insert(vehicle);

        log.info("新增车辆: {}", vehicle.getPlateNumber());
        return vehicle;
    }

    @Override
    @Transactional
    public int batchCreate(List<VehicleDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (VehicleDTO dto : dtos) {
            Vehicle vehicle = new Vehicle();
            BeanUtils.copyProperties(dto, vehicle);
            vehicle.setStatus(1);
            vehicleMapper.insert(vehicle);
            count++;
        }
        log.info("批量新增车辆 {} 条", count);
        return count;
    }

    @Override
    @Transactional
    public int importFromExcel(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        List<VehicleImportDTO> rows = EasyExcel.read(file.getInputStream())
                .head(VehicleImportDTO.class)
                .sheet()
                .doReadSync();

        int count = 0;
        for (VehicleImportDTO d : rows) {
            if (d == null || d.getPlateNumber() == null || d.getPlateNumber().isBlank()) {
                continue; // 跳过空行
            }
            Vehicle vehicle = new Vehicle();
            BeanUtils.copyProperties(d, vehicle);
            vehicle.setStatus(1);
            vehicleMapper.insert(vehicle);
            count++;
        }
        log.info("Excel 导入车辆 {} 条", count);
        return count;
    }

    @Override
    @Transactional
    public Vehicle update(Integer id, VehicleDTO dto) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new RuntimeException("车辆不存在");
        }

        BeanUtils.copyProperties(dto, vehicle);
        vehicleMapper.updateById(vehicle);

        log.info("更新车辆: {}", vehicle.getPlateNumber());
        return vehicle;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new RuntimeException("车辆不存在");
        }
        vehicle.setStatus(0);  // 软删除
        vehicleMapper.updateById(vehicle);

        log.info("注销车辆: {}", vehicle.getPlateNumber());
    }

    @Override
    @Transactional
    public InsuranceHistory renewInsurance(Integer vehicleId, RenewInsuranceDTO dto, MultipartFile attachment) {
        // 1. 将旧保险归档
        insuranceHistoryMapper.markAsHistory(vehicleId);

        // 2. 写入新保险记录
        InsuranceHistory newInsurance = InsuranceHistory.builder()
                .vehicleId(vehicleId)
                .insuranceCompany(dto.getInsuranceCompany())
                .insuranceType(dto.getInsuranceType())
                .policyNumber(dto.getPolicyNumber())
                .insuranceExpire(dto.getInsuranceExpire())
                .premium(dto.getPremium())
                .isCurrent(1)
                .build();
        insuranceHistoryMapper.insert(newInsurance);

        // 3. 同步更新车辆主表的保险信息
        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        vehicle.setInsuranceCompany(dto.getInsuranceCompany());
        vehicle.setInsuranceType(dto.getInsuranceType());
        vehicle.setPolicyNumber(dto.getPolicyNumber());
        vehicle.setInsuranceExpire(dto.getInsuranceExpire());
        vehicleMapper.updateById(vehicle);

        // 4. 将相关提醒标记为已处理
        // TODO: 更新提醒状态

        log.info("车辆 {} 续保成功，新保单截止: {}", vehicle.getPlateNumber(), dto.getInsuranceExpire());
        return newInsurance;
    }

    @Override
    @Transactional
    public InspectionHistory updateInspection(Integer vehicleId, java.time.LocalDate inspectionDate,
                                               java.time.LocalDate expireDate, MultipartFile attachment) {
        // 1. 将旧年检归档
        inspectionHistoryMapper.markAsHistory(vehicleId);

        // 2. 写入新年检记录
        InspectionHistory newInspection = InspectionHistory.builder()
                .vehicleId(vehicleId)
                .inspectionDate(inspectionDate)
                .expireDate(expireDate)
                .isCurrent(1)
                .build();
        inspectionHistoryMapper.insert(newInspection);

        // 3. 同步更新车辆主表
        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        vehicle.setInspectionExpire(expireDate);
        vehicleMapper.updateById(vehicle);

        log.info("车辆 {} 年检更新成功，下次到期: {}", vehicle.getPlateNumber(), expireDate);
        return newInspection;
    }
}
