package com.wansheng.vehicle.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wansheng.vehicle.dto.ApiResponse;
import com.wansheng.vehicle.entity.SystemConfig;
import com.wansheng.vehicle.repository.SystemConfigMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 系统配置 — 统一管理提醒接收邮箱等系统级配置。
 */
@Tag(name = "系统配置", description = "统一提醒邮箱等系统级配置")
@RestController
@RequestMapping("/api/system-config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigMapper systemConfigMapper;

    @Operation(summary = "获取统一提醒邮箱")
    @GetMapping("/notify-email")
    public ApiResponse<String> getNotifyEmail() {
        return ApiResponse.success(findValue("notify_email"));
    }

    @Operation(summary = "保存统一提醒邮箱（仅管理员）")
    @PutMapping("/notify-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> saveNotifyEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        upsert("notify_email", email, "统一提醒接收邮箱");
        return ApiResponse.success(null, "保存成功");
    }

    private String findValue(String key) {
        SystemConfig c = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        return c == null ? null : c.getConfigValue();
    }

    private void upsert(String key, String value, String desc) {
        SystemConfig c = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        if (c == null) {
            c = new SystemConfig();
            c.setConfigKey(key);
            c.setDescription(desc);
        }
        c.setConfigValue(value);
        c.setUpdatedAt(LocalDateTime.now());
        if (c.getId() == null) {
            systemConfigMapper.insert(c);
        } else {
            systemConfigMapper.updateById(c);
        }
    }
}
