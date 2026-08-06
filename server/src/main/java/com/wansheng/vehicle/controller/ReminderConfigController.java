package com.wansheng.vehicle.controller;

import com.wansheng.vehicle.dto.ApiResponse;
import com.wansheng.vehicle.entity.ReminderConfig;
import com.wansheng.vehicle.repository.ReminderConfigMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 提醒规则配置 API
 */
@Tag(name = "提醒规则配置", description = "保险/年检提醒节点与提醒方式管理")
@RestController
@RequestMapping("/api/reminder-config")
@RequiredArgsConstructor
public class ReminderConfigController {

    private final ReminderConfigMapper reminderConfigMapper;

    @Operation(summary = "获取全部提醒规则")
    @GetMapping
    public ApiResponse<List<ReminderConfig>> list() {
        List<ReminderConfig> configs = reminderConfigMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReminderConfig>()
                        .orderByAsc(ReminderConfig::getType)
                        .orderByDesc(ReminderConfig::getNodeDays)
        );
        return ApiResponse.success(configs);
    }

    @Operation(summary = "保存提醒规则配置（整表覆盖）")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> save(@RequestBody ReminderConfigRequest request) {
        // 1. 清空现有配置
        reminderConfigMapper.delete(null);

        // 2. 写入保险节点
        if (request.getInsuranceNodes() != null) {
            for (Integer nodeDays : request.getInsuranceNodes()) {
                ReminderConfig config = ReminderConfig.builder()
                        .type(0)
                        .nodeDays(nodeDays)
                        .enabled(1)
                        .remindMethods(joinMethods(request.getRemindMethods()))
                        .build();
                reminderConfigMapper.insert(config);
            }
        }

        // 3. 写入年检节点
        if (request.getInspectionNodes() != null) {
            for (Integer nodeDays : request.getInspectionNodes()) {
                ReminderConfig config = ReminderConfig.builder()
                        .type(1)
                        .nodeDays(nodeDays)
                        .enabled(1)
                        .remindMethods(joinMethods(request.getRemindMethods()))
                        .build();
                reminderConfigMapper.insert(config);
            }
        }

        return ApiResponse.success(null, "提醒规则保存成功");
    }

    private String joinMethods(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            return "system";
        }
        return methods.stream().distinct().collect(Collectors.joining(","));
    }

    @lombok.Data
    public static class ReminderConfigRequest {
        private List<Integer> insuranceNodes;
        private List<Integer> inspectionNodes;
        private List<String> remindMethods;
    }
}
