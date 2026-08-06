package com.wansheng.vehicle.controller;

import com.wansheng.vehicle.dto.ApiResponse;
import com.wansheng.vehicle.entity.Reminder;
import com.wansheng.vehicle.repository.ReminderMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒中心 API
 */
@Tag(name = "提醒中心", description = "到期提醒查询与处理")
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderMapper reminderMapper;

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

        return ApiResponse.success(reminderMapper.selectList(wrapper));
    }

    @Operation(summary = "标记提醒为已处理")
    @PutMapping("/{id}/handle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> handle(@PathVariable Integer id) {
        Reminder reminder = reminderMapper.selectById(id);
        if (reminder != null) {
            reminder.setStatus(1);  // 已处理
            reminder.setHandledAt(LocalDateTime.now());
            reminderMapper.updateById(reminder);
        }
        return ApiResponse.success(null, "已标记为处理");
    }
}
