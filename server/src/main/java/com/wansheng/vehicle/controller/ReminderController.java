package com.wansheng.vehicle.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wansheng.vehicle.dto.ApiResponse;
import com.wansheng.vehicle.dto.PageResult;
import com.wansheng.vehicle.entity.Reminder;
import com.wansheng.vehicle.entity.Vehicle;
import com.wansheng.vehicle.repository.ReminderMapper;
import com.wansheng.vehicle.repository.VehicleMapper;
import com.wansheng.vehicle.task.ReminderScheduledTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 提醒中心 API
 */
@Tag(name = "提醒中心", description = "到期提醒查询与处理")
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderMapper reminderMapper;
    private final VehicleMapper vehicleMapper;
    private final ReminderScheduledTask reminderScheduledTask;

    @Operation(summary = "获取提醒列表")
    @GetMapping
    public ApiResponse<PageResult<Reminder>> list(
            @RequestParam(defaultValue = "todo") String scope,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<Reminder> wrapper = new LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getArchived, 0);

        boolean history = "history".equalsIgnoreCase(scope);
        if (history) {
            wrapper.eq(Reminder::getStatus, 1);
        } else if (status != null && (status == 0 || status == 2)) {
            wrapper.eq(Reminder::getStatus, status);
        } else {
            wrapper.in(Reminder::getStatus, 0, 2);
        }

        if (type != null) {
            wrapper.eq(Reminder::getType, type);
        }
        if (startDate != null) {
            wrapper.ge(Reminder::getRemindDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(Reminder::getRemindDate, endDate);
        }

        if (keyword != null && !keyword.isBlank()) {
            List<Integer> vehicleIds = vehicleMapper.selectList(
                            new LambdaQueryWrapper<Vehicle>()
                                    .like(Vehicle::getPlateNumber, keyword.trim()))
                    .stream()
                    .map(Vehicle::getId)
                    .toList();
            if (vehicleIds.isEmpty()) {
                return ApiResponse.success(PageResult.of(
                        Collections.emptyList(), 0, safePage, safeSize));
            }
            wrapper.in(Reminder::getVehicleId, vehicleIds);
        }

        if (history) {
            wrapper.orderByDesc(Reminder::getHandledAt)
                    .orderByDesc(Reminder::getId);
        } else {
            wrapper.orderByDesc(Reminder::getStatus)
                    .orderByAsc(Reminder::getExpireDate)
                    .orderByDesc(Reminder::getRemindDate);
        }

        Page<Reminder> result = reminderMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        fillDisplayFields(result.getRecords());
        return ApiResponse.success(PageResult.of(
                result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize()));
    }

    @Operation(summary = "手动触发到期扫描")
    @PostMapping("/scan")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> scan() {
        reminderScheduledTask.scanExpiringVehicles();
        return ApiResponse.success(null, "扫描完成");
    }

    @Operation(summary = "标记提醒为已处理")
    @PutMapping("/{id}/handle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> handle(@PathVariable Integer id) {
        Reminder reminder = reminderMapper.selectById(id);
        if (reminder == null) {
            return ApiResponse.error("提醒不存在");
        }
        if (Integer.valueOf(1).equals(reminder.getStatus())) {
            return ApiResponse.success(null, "该提醒已经处理");
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        reminder.setStatus(1);  // 已处理
        reminder.setHandler(username);
        reminder.setHandledAt(LocalDateTime.now());
        reminderMapper.updateById(reminder);
        return ApiResponse.success(null, "已标记为处理");
    }

    private void fillDisplayFields(List<Reminder> reminders) {
        if (reminders.isEmpty()) {
            return;
        }
        List<Integer> vehicleIds = reminders.stream()
                .map(Reminder::getVehicleId)
                .distinct()
                .collect(Collectors.toList());

        List<Vehicle> vehicles = vehicleMapper.selectBatchIds(vehicleIds);
        Map<Integer, String> plateMap = vehicles.stream()
                .collect(Collectors.toMap(Vehicle::getId, Vehicle::getPlateNumber));

        LocalDate today = LocalDate.now();
        reminders.forEach(reminder -> {
            reminder.setPlateNumber(plateMap.get(reminder.getVehicleId()));
            if (reminder.getExpireDate() == null
                    && reminder.getRemindDate() != null
                    && reminder.getNodeDays() != null) {
                reminder.setExpireDate(reminder.getRemindDate().plusDays(reminder.getNodeDays()));
            }
            if (reminder.getExpireDate() == null) {
                reminder.setExpireStatus("未设置截止日期");
                return;
            }
            long remainingDays = ChronoUnit.DAYS.between(today, reminder.getExpireDate());
            reminder.setRemainingDays(remainingDays);
            if (remainingDays < 0) {
                reminder.setExpireStatus("已过期 " + Math.abs(remainingDays) + " 天");
            } else if (remainingDays == 0) {
                reminder.setExpireStatus("今日到期");
            } else {
                reminder.setExpireStatus("剩余 " + remainingDays + " 天");
            }
        });
    }
}
