package com.wansheng.vehicle.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wansheng.vehicle.entity.OperationLog;
import com.wansheng.vehicle.entity.Reminder;
import com.wansheng.vehicle.entity.ReminderConfig;
import com.wansheng.vehicle.entity.SystemConfig;
import com.wansheng.vehicle.entity.Vehicle;
import com.wansheng.vehicle.repository.OperationLogMapper;
import com.wansheng.vehicle.repository.ReminderConfigMapper;
import com.wansheng.vehicle.repository.ReminderMapper;
import com.wansheng.vehicle.repository.SystemConfigMapper;
import com.wansheng.vehicle.repository.VehicleMapper;
import com.wansheng.vehicle.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时任务 — 每日到期扫描
 *
 * 🎯 每天凌晨 2:00 执行：
 *    扫描所有车辆的保险和年检到期日期，
 *    根据 reminder_config 表配置的提醒节点生成提醒记录，
 *    并按提醒方式发送系统内消息或邮件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduledTask {

    private final VehicleMapper vehicleMapper;
    private final ReminderMapper reminderMapper;
    private final ReminderConfigMapper reminderConfigMapper;
    private final OperationLogMapper operationLogMapper;
    private final MailService mailService;
    private final SystemConfigMapper systemConfigMapper;

    /** 统一收件邮箱（扫描开始时从 system_config 读取一次） */
    private String notifyEmail;

    /** 兜底提醒节点：提前30/15/7/3天（当数据库无配置时使用） */
    private static final int[] DEFAULT_NODES = {30, 15, 7, 3};

    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点
    public void scheduledScan() {
        log.info("===== 开始每日到期扫描 =====");
        scanExpiringVehicles();
        log.info("===== 到期扫描完成 =====");
    }

    @Transactional
    public synchronized void scanExpiringVehicles() {
        this.notifyEmail = resolveNotifyEmail();
        LocalDate today = LocalDate.now();

        int archived = reminderMapper.archiveHandledBefore(LocalDateTime.now().minusYears(3));
        if (archived > 0) {
            log.info("已归档三年前的已处理提醒 {} 条", archived);
        }

        // 车辆的到期日期可能被编辑、续保或更新年检。先清理仍指向旧截止日期的
        // 待处理/超时未处理提醒，避免提醒中心继续展示已经失效的数据。
        removeStaleUnresolvedReminders();
        markTimedOutReminders(today);

        List<ReminderConfig> insuranceConfigs = loadEnabledConfigs(0);
        List<ReminderConfig> inspectionConfigs = loadEnabledConfigs(1);

        // 扫描全部有效车辆，而不是只查“今天刚好命中节点”的车辆。
        // 这样新录入、日期刚修改或服务停机后恢复的车辆，即使错过了 30 天节点，
        // 仍能在下一次扫描时补生成当前应该生效的提醒。
        List<Vehicle> activeVehicles = vehicleMapper.selectList(
                new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getStatus, 1));
        for (Vehicle vehicle : activeVehicles) {
            createApplicableReminder(vehicle, 0, today, insuranceConfigs);
            createApplicableReminder(vehicle, 1, today, inspectionConfigs);
        }
    }

    private void createApplicableReminder(Vehicle vehicle, int type, LocalDate today,
                                          List<ReminderConfig> configs) {
        LocalDate expireDate = type == 0
                ? vehicle.getInsuranceExpire()
                : vehicle.getInspectionExpire();
        if (expireDate == null) {
            return;
        }

        long remainingDays = ChronoUnit.DAYS.between(today, expireDate);
        ReminderConfig applicable = configs.stream()
                .filter(config -> config.getNodeDays() != null && config.getNodeDays() >= 0)
                .filter(config -> remainingDays <= config.getNodeDays())
                .min(Comparator.comparingInt(ReminderConfig::getNodeDays))
                .orElse(null);
        if (applicable == null) {
            return;
        }

        createReminder(vehicle, type, applicable.getNodeDays(), today,
                normalizeMethods(applicable.getRemindMethods()));
    }

    private void markTimedOutReminders(LocalDate today) {
        List<Reminder> pendingReminders = reminderMapper.findPendingReminders();
        for (Reminder r : pendingReminders) {
            if (r.getRemindDate().isBefore(today)) {
                r.setStatus(2);  // 提醒日期已过，但事项仍未处理
                reminderMapper.updateById(r);
            }
        }
    }

    private void removeStaleUnresolvedReminders() {
        List<Reminder> unresolved = reminderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Reminder>()
                        .in(Reminder::getStatus, 0, 2)
                        .eq(Reminder::getArchived, 0)
        );

        for (Reminder reminder : unresolved) {
            Vehicle vehicle = vehicleMapper.selectById(reminder.getVehicleId());
            if (!matchesCurrentExpiry(reminder, vehicle)) {
                reminderMapper.deleteById(reminder.getId());
                log.info("删除失效提醒: reminderId={}, vehicleId={}, type={}, remindDate={}",
                        reminder.getId(), reminder.getVehicleId(), reminder.getType(), reminder.getRemindDate());
            }
        }
    }

    private boolean matchesCurrentExpiry(Reminder reminder, Vehicle vehicle) {
        if (vehicle == null || !Integer.valueOf(1).equals(vehicle.getStatus())
                || reminder.getType() == null || reminder.getNodeDays() == null
                || reminder.getRemindDate() == null) {
            return false;
        }

        LocalDate expiry;
        if (reminder.getType() == 0) {
            expiry = vehicle.getInsuranceExpire();
        } else if (reminder.getType() == 1) {
            expiry = vehicle.getInspectionExpire();
        } else {
            return false;
        }

        LocalDate reminderExpiry = reminder.getExpireDate();
        if (reminderExpiry == null) {
            reminderExpiry = reminder.getRemindDate().plusDays(reminder.getNodeDays());
        }
        return expiry != null && expiry.equals(reminderExpiry);
    }

    private List<ReminderConfig> loadEnabledConfigs(Integer type) {
        List<ReminderConfig> configs = reminderConfigMapper.findEnabledByType(type);
        if (configs == null || configs.isEmpty()) {
            return Arrays.stream(DEFAULT_NODES)
                    .mapToObj(node -> ReminderConfig.builder()
                            .type(type)
                            .nodeDays(node)
                            .enabled(1)
                            .remindMethods("system,email")
                            .build())
                    .collect(Collectors.toList());
        }
        return configs;
    }

    private String normalizeMethods(String methods) {
        return methods == null || methods.isBlank() ? "system,email" : methods;
    }

    private void createReminder(Vehicle v, int type, int nodeDays, LocalDate today, String remindMethod) {
        LocalDate expireDate = type == 0 ? v.getInsuranceExpire() : v.getInspectionExpire();
        if (expireDate == null) {
            return;
        }

        Reminder existing = reminderMapper.findByCycle(v.getId(), type, expireDate);
        if (existing == null) {
            Reminder reminder = Reminder.builder()
                    .vehicleId(v.getId())
                    .type(type)
                    .nodeDays(nodeDays)
                    .remindDate(today)
                    .expireDate(expireDate)
                    .remindMethod(remindMethod)
                    .status(0)
                    .archived(0)
                    .build();
            reminderMapper.insert(reminder);
            log.info("创建提醒周期: vehicleId={}, plate={}, type={}, expireDate={}, nodeDays={}",
                    v.getId(), v.getPlateNumber(), type, expireDate, nodeDays);
            sendByConfiguredMethods(remindMethod, v, type, nodeDays, today);
            return;
        }

        // 已处理表示本轮事项已经完成，后续提醒节点不再重复通知。
        if (Integer.valueOf(1).equals(existing.getStatus())) {
            return;
        }

        // 当前节点已经提醒过，后续每天扫描都不重复更新或发送。
        // 节点只允许从 30 → 15 → 7 → 3 向更紧急方向推进。
        if (existing.getNodeDays() != null && nodeDays >= existing.getNodeDays()) {
            return;
        }

        existing.setNodeDays(nodeDays);
        existing.setRemindDate(today);
        existing.setExpireDate(expireDate);
        existing.setRemindMethod(remindMethod);
        existing.setArchived(0);
        reminderMapper.updateById(existing);
        log.info("升级提醒节点: reminderId={}, vehicleId={}, type={}, expireDate={}, nodeDays={}",
                existing.getId(), v.getId(), type, expireDate, nodeDays);
        sendByConfiguredMethods(remindMethod, v, type, nodeDays, today);
    }

    /**
     * 按提醒方式发送提醒。
     * - system：提醒已落库到 reminders 表，前端"提醒中心"可见，这里仅记录日志。
     * - email ：通过 MailService 真实调用 QQ 邮箱 SMTP 发送邮件到统一收件邮箱。
     * - sms 等其他方式：已不再支持，忽略。
     */
    private void sendByConfiguredMethods(String methods, Vehicle v, int type,
                                         int nodeDays, LocalDate today) {
        if (methods == null || methods.isBlank()) {
            return;
        }
        String plate = v.getPlateNumber() == null ? ("车辆#" + v.getId()) : v.getPlateNumber();
        String typeName = typeLabel(type);
        LocalDate expireDate = type == 0 ? v.getInsuranceExpire() : v.getInspectionExpire();
        int remainingDays = expireDate == null
                ? nodeDays
                : Math.toIntExact(ChronoUnit.DAYS.between(today, expireDate));

        for (String raw : methods.split(",")) {
            String method = raw.trim();
            if ("system".equals(method)) {
                log.info("[系统内提醒] 车辆 {} 的{}进入提前 {} 天节点，当前剩余 {} 天",
                        plate, typeName, nodeDays, remainingDays);
            } else if ("email".equals(method)) {
                String desc;
                if (notifyEmail != null && !notifyEmail.isBlank()) {
                    boolean sent = mailService.sendReminder(
                            notifyEmail, plate, typeName, nodeDays, remainingDays);
                    desc = sent
                            ? String.format("已发送邮件提醒：车辆 %s 的%s进入提前 %d 天节点，当前剩余 %d 天，收件人 %s",
                                    plate, typeName, nodeDays, remainingDays, notifyEmail)
                            : String.format("邮件提醒发送失败：车辆 %s 的%s进入提前 %d 天节点，当前剩余 %d 天，收件人 %s",
                                    plate, typeName, nodeDays, remainingDays, notifyEmail);
                    if (sent) log.info("[邮件发送] {}", desc); else log.warn("[邮件发送] {}", desc);
                } else {
                    desc = String.format(
                            "未发送邮件提醒（未配置统一收件邮箱）：车辆 %s 的%s进入提前 %d 天节点，当前剩余 %d 天",
                            plate, typeName, nodeDays, remainingDays);
                    log.warn("[邮件发送] {}", desc);
                }
                operationLogMapper.insert(OperationLog.builder()
                        .userName("系统定时任务")
                        .vehicleId(v.getId())
                        .action(desc.startsWith("已发送") ? "EMAIL_REMINDER" : "EMAIL_REMINDER_FAILED")
                        .description(desc)
                        .build());
            }
            // sms 等其他方式：已不再支持，忽略
        }
    }

    /** 从 system_config 读取统一收件邮箱 */
    private String resolveNotifyEmail() {
        SystemConfig c = systemConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getConfigKey, "notify_email"));
        return c == null ? null : c.getConfigValue();
    }

    private String typeLabel(int type) {
        return type == 0 ? "保险" : "年检";
    }
}
