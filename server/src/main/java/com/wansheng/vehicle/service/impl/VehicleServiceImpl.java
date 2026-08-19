package com.wansheng.vehicle.service.impl;

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
import java.util.Objects;

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
        // “今日到期提醒”统计今天触发且尚未处理的提醒，不等同于今天到期的车辆。
        long todayExpiring = reminderMapper.selectCount(
                new LambdaQueryWrapper<Reminder>()
                        .eq(Reminder::getRemindDate, today)
                        .in(Reminder::getStatus, 0, 2)
                        .eq(Reminder::getArchived, 0)
        );
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
    public ImportResult importFromExcel(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        log.info("开始导入 Excel，文件名：{}，大小：{} bytes", originalName, file.getSize());

        List<Vehicle> vehicles = readVehiclesFromExcel(file);

        if (vehicles.isEmpty()) {
            throw new IllegalArgumentException(
                    "未从 Excel 中读取到任何有效数据。请检查：1) 是否存在包含“车牌号”的表头行；2) 表头字段名称是否正确；3) 数据行是否在表头下方；4) 文件是否为真实的 .xls/.xlsx 格式（而非 HTML 伪装的 .xls）。");
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getPlateNumber() == null || vehicle.getPlateNumber().isBlank()) {
                skipped++;
                continue;
            }

            // 默认值填充
            if (vehicle.getVehicleType() == null) {
                vehicle.setVehicleType(0); // Excel 未提供车辆类型时默认小车
            }
            if (vehicle.getCompany() == null || vehicle.getCompany().isBlank()) {
                vehicle.setCompany("万盛股份");
            }
            if (vehicle.getOwner() == null || vehicle.getOwner().isBlank()) {
                vehicle.setOwner("公司");
            }

            // 按车牌号查询所有现存记录（逻辑删除的会自动被过滤）
            List<Vehicle> existingList = vehicleMapper.selectList(
                    new LambdaQueryWrapper<Vehicle>()
                            .eq(Vehicle::getPlateNumber, vehicle.getPlateNumber())
                            .orderByAsc(Vehicle::getId)
            );

            if (!existingList.isEmpty()) {
                // 保留最新一条并覆盖为本次导入数据；若存在历史重复数据，将较早的记录标记为删除
                Vehicle latest = existingList.get(existingList.size() - 1);
                LocalDate oldInsuranceExpire = latest.getInsuranceExpire();
                LocalDate oldInspectionExpire = latest.getInspectionExpire();
                vehicle.setId(latest.getId());
                vehicle.setStatus(1);
                vehicleMapper.updateById(vehicle);
                clearUnresolvedRemindersIfExpiryChanged(
                        vehicle.getId(), 0, oldInsuranceExpire, vehicle.getInsuranceExpire());
                clearUnresolvedRemindersIfExpiryChanged(
                        vehicle.getId(), 1, oldInspectionExpire, vehicle.getInspectionExpire());
                updated++;
                log.info("更新车辆：{}（id={}）", vehicle.getPlateNumber(), latest.getId());

                for (int i = 0; i < existingList.size() - 1; i++) {
                    Integer duplicateId = existingList.get(i).getId();
                    reminderMapper.deleteUnresolvedByVehicleAndType(duplicateId, 0);
                    reminderMapper.deleteUnresolvedByVehicleAndType(duplicateId, 1);
                    vehicleMapper.deleteById(duplicateId);
                    log.info("清理历史重复车辆：{}（id={}）", vehicle.getPlateNumber(), duplicateId);
                }
            } else {
                vehicle.setStatus(1);
                vehicleMapper.insert(vehicle);
                inserted++;
                log.info("新增车辆：{}", vehicle.getPlateNumber());
            }
        }

        int total = inserted + updated;
        saveLog("IMPORT_VEHICLE", null,
                "Excel 导入车辆：新增 " + inserted + " 条，更新 " + updated + " 条，跳过 " + skipped + " 行",
                null, "{\"total\":" + total + ",\"inserted\":" + inserted + ",\"updated\":" + updated + ",\"skipped\":" + skipped + "}");
        log.info("Excel 导入完成，读取 {} 行，新增 {} 条，更新 {} 条，跳过 {} 行",
                vehicles.size(), inserted, updated, skipped);
        return new ImportResult(total, inserted, updated, skipped);
    }

    /**
     * 通用 Excel 解析：同时支持 .xls / .xlsx，自动定位“车牌号”所在表头行，按列名映射字段。
     */
    private List<Vehicle> readVehiclesFromExcel(MultipartFile file) throws IOException {
        List<Vehicle> result = new ArrayList<>();

        try (org.apache.poi.ss.usermodel.Workbook workbook =
                     org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel 中没有 sheet");
            }

            // 1. 定位表头行：在前 10 行中查找包含“车牌号”的单元格
            int headerRowIndex = -1;
            java.util.Map<Integer, String> headerMap = new java.util.HashMap<>();
            int lastRow = Math.min(10, sheet.getLastRowNum() + 1);
            for (int r = 0; r < lastRow; r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                if (row == null) continue;
                java.util.Map<Integer, String> temp = new java.util.HashMap<>();
                boolean hasPlate = false;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String val = getCellStringValue(row.getCell(c));
                    if (val != null && val.contains("车牌号")) {
                        hasPlate = true;
                    }
                    temp.put(c, val);
                }
                if (hasPlate) {
                    headerRowIndex = r;
                    headerMap = temp;
                    break;
                }
            }

            if (headerRowIndex == -1) {
                throw new IllegalArgumentException("未在 Excel 中找到包含“车牌号”的表头行，请检查表头内容。");
            }

            // 2. 建立列索引 -> 字段的映射
            java.util.Map<String, Integer> columnIndex = new java.util.HashMap<>();
            for (java.util.Map.Entry<Integer, String> entry : headerMap.entrySet()) {
                String header = normalizeHeader(entry.getValue());
                if (header != null) {
                    columnIndex.put(header, entry.getKey());
                }
            }

            // 3. 从表头下一行开始读取数据
            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                if (row == null) continue;

                String plate = getStringCell(row, columnIndex.get("车牌号"));
                if (plate == null || plate.isBlank()) {
                    continue; // 跳过空行
                }

                Vehicle v = new Vehicle();
                v.setPlateNumber(plate);
                v.setVehicleType(parseVehicleType(getStringCell(row, columnIndex.get("车辆类型"))));
                v.setBrand(getStringCell(row, columnIndex.get("品牌")));
                v.setPurchaseDate(getDateCell(row, columnIndex.get("上牌日期")));
                v.setCompany(getStringCell(row, columnIndex.get("所属公司")));
                v.setOwner(getStringCell(row, columnIndex.get("所属")));
                v.setInsuranceCompany(getStringCell(row, columnIndex.get("投保公司")));
                v.setInsuranceType(getStringCell(row, columnIndex.get("险种")));
                v.setPolicyNumber(getStringCell(row, columnIndex.get("保单号")));
                v.setInsuranceExpire(getDateCell(row, columnIndex.get("保险到期")));
                v.setInspectionExpire(getDateCell(row, columnIndex.get("年检到期")));
                v.setEtcBank(getStringCell(row, columnIndex.get("ETC银行")));
                v.setOilCardNumber(getStringCell(row, columnIndex.get("油卡号")));
                v.setRemark(getStringCell(row, columnIndex.get("备注")));

                result.add(v);
            }
        }
        return result;
    }

    private String normalizeHeader(String header) {
        if (header == null) return null;
        String h = header.trim().replaceAll("\\s+", "").replaceAll("[()（）0-9=]", "");
        if (h.contains("车牌号")) return "车牌号";
        if (h.contains("车辆类型") || h.equals("类型")) return "车辆类型";
        if (h.contains("车辆品牌") || h.equals("品牌")) return "品牌";
        if (h.contains("上牌时间") || h.contains("上牌日期") || h.contains("购买日期")) return "上牌日期";
        if (h.contains("所属公司") || h.equals("公司")) return "所属公司";
        if (h.contains("产权所属") || h.equals("所属") || h.equals("归属")) return "所属";
        if (h.contains("投保公司") || h.contains("保险公司")) return "投保公司";
        if (h.contains("险种") || h.contains("保险类型")) return "险种";
        if (h.contains("保单号")) return "保单号";
        if (h.contains("保险截止") || h.contains("保险到期") || h.contains("保险结束")) return "保险到期";
        if (h.contains("年检日期") || h.contains("年检截止") || h.contains("年检到期")) return "年检到期";
        if (h.contains("ETC办理") || h.contains("ETC银行") || h.equals("ETC")) return "ETC银行";
        if (h.contains("油卡号码") || h.contains("油卡号") || h.equals("油卡")) return "油卡号";
        if (h.contains("备忘录") || h.contains("备注")) return "备注";
        return null;
    }

    private Integer parseVehicleType(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        if (v.equals("0") || v.equals("小车") || v.contains("小")) return 0;
        if (v.equals("1") || v.equals("大巴") || v.contains("大")) return 1;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getStringCell(org.apache.poi.ss.usermodel.Row row, Integer col) {
        if (col == null) return null;
        return getCellStringValue(row.getCell(col));
    }

    private LocalDate getDateCell(org.apache.poi.ss.usermodel.Row row, Integer col) {
        if (col == null) return null;
        org.apache.poi.ss.usermodel.Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                    && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String s = getCellStringValue(cell);
            if (s == null || s.isBlank()) return null;
            // 支持 yyyy-MM-dd 或 yyyy/MM/dd
            s = s.trim().replace("/", "-");
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("日期解析失败，值：{}，原因：{}", cell, e.getMessage());
            return null;
        }
    }

    private String getCellStringValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
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
        clearUnresolvedRemindersIfExpiryChanged(
                vehicle.getId(), 0, before.getInsuranceExpire(), vehicle.getInsuranceExpire());
        clearUnresolvedRemindersIfExpiryChanged(
                vehicle.getId(), 1, before.getInspectionExpire(), vehicle.getInspectionExpire());

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
        reminderMapper.deleteUnresolvedByVehicleAndType(vehicle.getId(), 0);
        reminderMapper.deleteUnresolvedByVehicleAndType(vehicle.getId(), 1);

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

        // 4. 新保单日期生效后，旧保单产生的未处理/逾期提醒已经失效
        clearUnresolvedRemindersIfExpiryChanged(
                vehicleId, 0, before.getInsuranceExpire(), vehicle.getInsuranceExpire());

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
        clearUnresolvedRemindersIfExpiryChanged(
                vehicleId, 1, before.getInspectionExpire(), vehicle.getInspectionExpire());

        saveLog("UPDATE_INSPECTION", vehicleId,
                "车辆 " + vehicle.getPlateNumber() + " 更新年检，下次到期：" + expireDate,
                before, vehicle);
        log.info("车辆 {} 年检更新成功，下次到期: {}", vehicle.getPlateNumber(), expireDate);
        return newInspection;
    }

    private void clearUnresolvedRemindersIfExpiryChanged(
            Integer vehicleId, Integer type, LocalDate oldExpiry, LocalDate newExpiry) {
        if (Objects.equals(oldExpiry, newExpiry)) {
            return;
        }
        int deleted = reminderMapper.deleteUnresolvedByVehicleAndType(vehicleId, type);
        if (deleted > 0) {
            log.info("车辆到期日期已变化，清理失效提醒: vehicleId={}, type={}, count={}",
                    vehicleId, type, deleted);
        }
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
