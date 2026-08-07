package com.wansheng.vehicle.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wansheng.vehicle.dto.*;
import com.wansheng.vehicle.entity.*;
import com.wansheng.vehicle.repository.*;
import com.wansheng.vehicle.security.SecurityUtils;
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
 *    - 关键操作记录操作日志
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
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;

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

        saveLog("CREATE_VEHICLE", vehicle.getId(),
                "新增车辆：" + vehicle.getPlateNumber(),
                null, vehicle);
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

        saveLog("BATCH_CREATE_VEHICLE", null,
                "批量新增车辆 " + count + " 条",
                null, "{\"count\":" + count + "}");
        log.info("批量新增车辆 {} 条", count);
        return count;
    }

    @Override
    @Transactional
    public int importFromExcel(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        log.info("开始导入 Excel，文件名：{}，大小：{} bytes", originalName, file.getSize());

        List<VehicleImportDTO> rows = EasyExcel.read(file.getInputStream())
                .head(VehicleImportDTO.class)
                .sheet()
                .doReadSync();

        if (rows == null) {
            rows = new ArrayList<>();
        }
        log.info("EasyExcel 共读取到 {} 行数据", rows.size());

        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "未从 Excel 中读取到任何数据。请检查：1) 第一行是否为表头；2) 表头文字是否与模板一致（车牌号、车辆类型、品牌……）；3) 数据是否从第二行开始；4) 如为 .xls 格式，建议另存为 .xlsx 后重试。");
        }

        int count = 0;
        int skip = 0;
        for (int i = 0; i < rows.size(); i++) {
            VehicleImportDTO d = rows.get(i);
            if (d == null || d.getPlateNumber() == null || d.getPlateNumber().isBlank()) {
                skip++;
                log.warn("第 {} 行数据为空或车牌号缺失，已跳过", i + 2);
                continue; // 跳过空行
            }
            Vehicle vehicle = new Vehicle();
            BeanUtils.copyProperties(d, vehicle);
            if (vehicle.getVehicleType() == null) {
                vehicle.setVehicleType(0); // Excel 未提供车辆类型时默认小车
            }
            vehicle.setStatus(1);
            vehicleMapper.insert(vehicle);
            count++;
        }

        saveLog("IMPORT_VEHICLE", null,
                "Excel 导入车辆 " + count + " 条（读取 " + rows.size() + " 行，跳过 " + skip + " 行）",
                null, "{\"count\":" + count + ",\"read\":" + rows.size() + ",\"skip\":" + skip + "}");
        log.info("Excel 导入完成，读取 {} 行，跳过 {} 行，成功导入 {} 条", rows.size(), skip, count);
        return count;
    }

    @Override
    @Transactional
    public Vehicle update(Integer id, VehicleDTO dto) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new RuntimeException("车辆不存在");
        }

        Vehicle before = new Vehicle();
        BeanUtils.copyProperties(vehicle, before);

        BeanUtils.copyProperties(dto, vehicle);
        vehicleMapper.updateById(vehicle);

        saveLog("UPDATE_VEHICLE", vehicle.getId(),
                "更新车辆：" + vehicle.getPlateNumber(),
                before, vehicle);
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

        Vehicle before = new Vehicle();
        BeanUtils.copyProperties(vehicle, before);

        vehicle.setStatus(0);  // 软删除
        vehicleMapper.updateById(vehicle);

        saveLog("DELETE_VEHICLE", vehicle.getId(),
                "注销车辆：" + vehicle.getPlateNumber(),
                before, vehicle);
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
        Vehicle before = new Vehicle();
        BeanUtils.copyProperties(vehicle, before);

        vehicle.setInsuranceCompany(dto.getInsuranceCompany());
        vehicle.setInsuranceType(dto.getInsuranceType());
        vehicle.setPolicyNumber(dto.getPolicyNumber());
        vehicle.setInsuranceExpire(dto.getInsuranceExpire());
        vehicleMapper.updateById(vehicle);

        // 4. 将相关提醒标记为已处理
        // TODO: 更新提醒状态

        saveLog("RENEW_INSURANCE", vehicleId,
                "车辆 " + vehicle.getPlateNumber() + " 续保，新保单截止：" + dto.getInsuranceExpire(),
                before, vehicle);
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
        Vehicle before = new Vehicle();
        BeanUtils.copyProperties(vehicle, before);

        vehicle.setInspectionExpire(expireDate);
        vehicleMapper.updateById(vehicle);

        saveLog("UPDATE_INSPECTION", vehicleId,
                "车辆 " + vehicle.getPlateNumber() + " 更新年检，下次到期：" + expireDate,
                before, vehicle);
        log.info("车辆 {} 年检更新成功，下次到期: {}", vehicle.getPlateNumber(), expireDate);
        return newInspection;
    }

    // ──────────────────────────────────────
    //  操作日志辅助方法
    // ──────────────────────────────────────

    private void saveLog(String action, Integer vehicleId, String description, Object before, Object after) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            Integer userId = resolveUserId(username);
            String ip = SecurityUtils.getCurrentIpAddress();

            OperationLog operationLog = OperationLog.builder()
                    .userId(userId)
                    .userName(username != null ? username : "system")
                    .vehicleId(vehicleId)
                    .action(action)
                    .description(description)
                    .beforeData(before != null ? objectMapper.writeValueAsString(before) : null)
                    .afterData(after != null ? objectMapper.writeValueAsString(after) : null)
                    .ipAddress(ip)
                    .build();
            operationLogMapper.insert(operationLog);
        } catch (JsonProcessingException e) {
            log.warn("操作日志序列化失败: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("操作日志记录失败: {}", e.getMessage());
        }
    }

    private Integer resolveUserId(String username) {
        if (username == null) {
            return null;
        }
        try {
            SysUser user = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
            );
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
