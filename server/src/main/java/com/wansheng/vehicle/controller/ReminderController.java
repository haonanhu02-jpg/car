package com.wansheng.vehicle.controller;

import com.wansheng.vehicle.dto.ApiResponse;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    public ApiResponse<List<Reminder>> list(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Reminder> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        if (type != null) {
            wrapper.eq(Reminder::getType, type);
        }
        if (status != null) {
            wrapper.eq(Reminder::getStatus, status);
        }
        wrapper.orderByAsc(Reminder::getRemindDate);

        List<Reminder> reminders = reminderMapper.selectList(wrapper);
        fillPlateNumber(reminders);
        return ApiResponse.success(reminders);
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        reminder.setStatus(1);  // 已处理
        reminder.setHandler(username);
        reminder.setHandledAt(LocalDateTime.now());
        reminderMapper.updateById(reminder);
        return ApiResponse.success(null, "已标记为处理");
    }

    private void fillPlateNumber(List<Reminder> reminders) {
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

        reminders.forEach(r -> r.setPlateNumber(plateMap.get(r.getVehicleId())));
    }
}
