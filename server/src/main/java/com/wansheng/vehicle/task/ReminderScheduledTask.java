package com.wansheng.vehicle.task;

import com.wansheng.vehicle.entity.Reminder;
import com.wansheng.vehicle.entity.Vehicle;
import com.wansheng.vehicle.repository.ReminderMapper;
import com.wansheng.vehicle.repository.VehicleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 定时任务 — 每日到期扫描
 *
 * 🎯 每天凌晨 2:00 执行：
 *    扫描所有车辆的保险和年检到期日期，
 *    根据配置的提醒节点（30/15/7/3天前）生成提醒记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduledTask {

    private final VehicleMapper vehicleMapper;
    private final ReminderMapper reminderMapper;

    /** 默认提醒节点：提前30/15/7/3天 */
    private static final int[] REMINDER_NODES = {30, 15, 7, 3};

    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点
    public void scanExpiringVehicles() {
        log.info("===== 开始每日到期扫描 =====");
        LocalDate today = LocalDate.now();

        for (int nodeDays : REMINDER_NODES) {
            LocalDate targetDate = today.plusDays(nodeDays);

            // 查找保险在该天到期的车辆
            List<Vehicle> insuranceExpiring = vehicleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Vehicle>()
                    .eq(Vehicle::getInsuranceExpire, targetDate)
                    .eq(Vehicle::getStatus, 1)
            );

            for (Vehicle v : insuranceExpiring) {
                createReminder(v.getId(), 0, nodeDays, today);
            }

            // 查找年检在该天到期的车辆
            List<Vehicle> inspectionExpiring = vehicleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Vehicle>()
                    .eq(Vehicle::getInspectionExpire, targetDate)
                    .eq(Vehicle::getStatus, 1)
            );

            for (Vehicle v : inspectionExpiring) {
                createReminder(v.getId(), 1, nodeDays, today);
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

        log.info("===== 到期扫描完成 =====");
    }

    private void createReminder(Integer vehicleId, int type, int nodeDays, LocalDate today) {
        // 检查是否已存在相同提醒
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
                    .remindMethod("system")
                    .status(0)
                    .build();
            reminderMapper.insert(reminder);
            log.debug("创建提醒: vehicleId={}, type={}, nodeDays={}", vehicleId, type, nodeDays);
        }
    }
}
