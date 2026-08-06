package com.wansheng.vehicle.task;

import com.wansheng.vehicle.entity.OperationLog;
import com.wansheng.vehicle.entity.Reminder;
import com.wansheng.vehicle.entity.ReminderConfig;
import com.wansheng.vehicle.entity.Vehicle;
import com.wansheng.vehicle.repository.OperationLogMapper;
import com.wansheng.vehicle.repository.ReminderConfigMapper;
import com.wansheng.vehicle.repository.ReminderMapper;
import com.wansheng.vehicle.repository.VehicleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时任务 — 每日到期扫描
 *
 * 🎯 每天凌晨 2:00 执行：
 *    扫描所有车辆的保险和年检到期日期，
 *    根据 reminder_config 表配置的提醒节点生成提醒记录，
 *    并按提醒方式（系统内 / 邮件）做"模拟发送"。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduledTask {

    private final VehicleMapper vehicleMapper;
    private final ReminderMapper reminderMapper;
    private final ReminderConfigMapper reminderConfigMapper;
    private final OperationLogMapper operationLogMapper;

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
                createReminder(v, 0, nodeDays, today, resolveRemindMethod(0));
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
                createReminder(v, 1, nodeDays, today, resolveRemindMethod(1));
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
            return Arrays.stream(DEFAULT_NODES).boxed().collect(Collectors.toList());
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

    private void createReminder(Vehicle v, int type, int nodeDays, LocalDate today, String remindMethod) {
        // 检查是否已存在相同提醒（同一辆车、同一类型、同一节点、同一天）
        List<Reminder> existing = reminderMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getVehicleId, v.getId())
                .eq(Reminder::getType, type)
                .eq(Reminder::getNodeDays, nodeDays)
                .eq(Reminder::getRemindDate, today)
        );

        if (existing.isEmpty()) {
            Reminder reminder = Reminder.builder()
                    .vehicleId(v.getId())
                    .type(type)
                    .nodeDays(nodeDays)
                    .remindDate(today)
                    .remindMethod(remindMethod)
                    .status(0)
                    .build();
            reminderMapper.insert(reminder);
            log.info("创建系统内提醒: vehicleId={}, plate={}, type={}, nodeDays={}",
                    v.getId(), v.getPlateNumber(), type, nodeDays);
            // 按提醒方式做"模拟发送"
            simulateSend(remindMethod, v, type, nodeDays);
        }
    }

    /**
     * 模拟发送提醒。
     * - system：提醒已落库到 reminders 表，前端"提醒中心"可见，这里仅记录日志。
     * - email ：不真正调用邮件服务，记录一条 operation_log 并在控制台打印，便于查看"已发送"。
     * - sms 等其他方式：已不再支持，忽略。
     */
    private void simulateSend(String methods, Vehicle v, int type, int nodeDays) {
        if (methods == null || methods.isBlank()) {
            return;
        }
        String plate = v.getPlateNumber() == null ? ("车辆#" + v.getId()) : v.getPlateNumber();
        String typeName = typeLabel(type);

        for (String raw : methods.split(",")) {
            String method = raw.trim();
            if ("system".equals(method)) {
                log.info("[系统内提醒] 车辆 {} 的{}将在 {} 天后到期", plate, typeName, nodeDays);
            } else if ("email".equals(method)) {
                String desc = String.format(
                        "已模拟发送邮件提醒：车辆 %s 的%s将在 %d 天后到期（提前 %d 天节点）",
                        plate, typeName, nodeDays, nodeDays);
                log.info("[邮件模拟发送] {}", desc);
                operationLogMapper.insert(OperationLog.builder()
                        .userName("系统定时任务")
                        .vehicleId(v.getId())
                        .action("EMAIL_REMINDER")
                        .description(desc)
                        .build());
            }
            // sms 等其他方式：已不再支持，忽略
        }
    }

    private String typeLabel(int type) {
        return type == 0 ? "保险" : "年检";
    }
}
