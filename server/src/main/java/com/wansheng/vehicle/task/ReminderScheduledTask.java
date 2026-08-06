package com.wansheng.vehicle.task;

import com.wansheng.vehicle.entity.Reminder;
import com.wansheng.vehicle.entity.ReminderConfig;
import com.wansheng.vehicle.entity.Vehicle;
import com.wansheng.vehicle.repository.ReminderConfigMapper;
import com.wansheng.vehicle.repository.ReminderMapper;
import com.wansheng.vehicle.repository.VehicleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时任务 — 每日到期扫描
 *
 * 🎯 每天凌晨 2:00 执行：
 *    扫描所有车辆的保险和年检到期日期，
 *    根据 reminder_config 表配置的提醒节点生成提醒记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduledTask {

    private final VehicleMapper vehicleMapper;
    private final ReminderMapper reminderMapper;
    private final ReminderConfigMapper reminderConfigMapper;

    /** 兜底提醒节点：提前30/15/7/3天（当数据库无配置时使用） */
    private static final int[] DEFAULT_NODES = {30, 15, 7, 3};

    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点
    public void scheduledScan() {
        log.info("===== 开始每日到期扫描 =====");
        scanExpiringVehicles();
        log.info("===== 到期扫描完成 =====");
    }

    @Transactional
    public void scanExpiringVehicles() {
        LocalDate today = LocalDate.now();

        List<Integer> insuranceNodes = loadEnabledNodeDays(0);
        List<Integer> inspectionNodes = loadEnabledNodeDays(1);

        // 保险提醒
        for (int nodeDays : insuranceNodes) {
            LocalDate targetDate = today.plusDays(nodeDays);
            List<Vehicle> insuranceExpiring = vehicleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Vehicle>()
                    .eq(Vehicle::getInsuranceExpire, targetDate)
                    .eq(Vehicle::getStatus, 1)
            );

            for (Vehicle v : insuranceExpiring) {
                createReminder(v.getId(), 0, nodeDays, today, resolveRemindMethod(0));
            }
        }

        // 年检提醒
        for (int nodeDays : inspectionNodes) {
            LocalDate targetDate = today.plusDays(nodeDays);
            List<Vehicle> inspectionExpiring = vehicleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Vehicle>()
                    .eq(Vehicle::getInspectionExpire, targetDate)
                    .eq(Vehicle::getStatus, 1)
            );

            for (Vehicle v : inspectionExpiring) {
                createReminder(v.getId(), 1, nodeDays, today, resolveRemindMethod(1));
            }
        }

        // 扫描已逾期的提醒
        List<Reminder> pendingReminders = reminderMapper.findPendingReminders();
        for (Reminder r : pendingReminders) {
            if (r.getRemindDate().isBefore(today)) {
                r.setStatus(2);  // 标记为已逾期
                reminderMapper.updateById(r);
            }
        }
    }

    private List<Integer> loadEnabledNodeDays(Integer type) {
        List<ReminderConfig> configs = reminderConfigMapper.findEnabledByType(type);
        if (configs == null || configs.isEmpty()) {
            return type == 0
                    ? java.util.Arrays.stream(DEFAULT_NODES).boxed().collect(Collectors.toList())
                    : java.util.Arrays.stream(DEFAULT_NODES).boxed().collect(Collectors.toList());
        }
        return configs.stream()
                .map(ReminderConfig::getNodeDays)
                .collect(Collectors.toList());
    }

    private String resolveRemindMethod(Integer type) {
        List<ReminderConfig> configs = reminderConfigMapper.findEnabledByType(type);
        if (configs == null || configs.isEmpty()) {
            return "system";
        }
        String methods = configs.get(0).getRemindMethods();
        return methods == null || methods.isBlank() ? "system" : methods;
    }

    private void createReminder(Integer vehicleId, int type, int nodeDays, LocalDate today, String remindMethod) {
        // 检查是否已存在相同提醒（同一辆车、同一类型、同一节点、同一天）
        List<Reminder> existing = reminderMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getVehicleId, vehicleId)
                .eq(Reminder::getType, type)
                .eq(Reminder::getNodeDays, nodeDays)
                .eq(Reminder::getRemindDate, today)
        );

        if (existing.isEmpty()) {
            Reminder reminder = Reminder.builder()
                    .vehicleId(vehicleId)
                    .type(type)
                    .nodeDays(nodeDays)
                    .remindDate(today)
                    .remindMethod(remindMethod)
                    .status(0)
                    .build();
            reminderMapper.insert(reminder);
            log.debug("创建提醒: vehicleId={}, type={}, nodeDays={}", vehicleId, type, nodeDays);
        }
    }
}
