package com.wansheng.vehicle.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wansheng.vehicle.dto.ApiResponse;
import com.wansheng.vehicle.dto.PageResult;
import com.wansheng.vehicle.entity.OperationLog;
import com.wansheng.vehicle.repository.OperationLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志 API（仅管理员）
 */
@Tag(name = "操作日志", description = "系统操作日志查询与审计")
@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OperationLogController {

    private final OperationLogMapper operationLogMapper;

    @Operation(summary = "分页查询操作日志")
    @GetMapping
    public ApiResponse<PageResult<OperationLog>> list(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        if (userName != null && !userName.isBlank()) {
            wrapper.like(OperationLog::getUserName, userName);
        }
        if (action != null && !action.isBlank()) {
            wrapper.eq(OperationLog::getAction, action);
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getCreatedAt, endTime);
        }

        wrapper.orderByDesc(OperationLog::getCreatedAt);

        Page<OperationLog> result = operationLogMapper.selectPage(new Page<>(page, size), wrapper);
        return ApiResponse.success(PageResult.of(
                result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize()
        ));
    }

    @Operation(summary = "获取所有操作类型")
    @GetMapping("/actions")
    public ApiResponse<List<String>> actions() {
        List<String> actions = List.of(
                "CREATE_VEHICLE", "UPDATE_VEHICLE", "DELETE_VEHICLE",
                "BATCH_CREATE_VEHICLE", "IMPORT_VEHICLE",
                "RENEW_INSURANCE", "UPDATE_INSPECTION",
                "CREATE_USER", "UPDATE_USER_STATUS", "RESET_USER_PASSWORD",
                "EMAIL_REMINDER"
        );
        return ApiResponse.success(actions);
    }
}
