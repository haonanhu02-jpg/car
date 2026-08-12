package com.wansheng.vehicle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wansheng.vehicle.dto.ApiResponse;
import com.wansheng.vehicle.entity.OperationLog;
import com.wansheng.vehicle.entity.SysUser;
import com.wansheng.vehicle.repository.OperationLogMapper;
import com.wansheng.vehicle.repository.SysUserMapper;
import com.wansheng.vehicle.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理 API（仅管理员）
 */
@Tag(name = "用户管理", description = "系统用户增删改查与状态管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final SysUserMapper sysUserMapper;
    private final OperationLogMapper operationLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Operation(summary = "用户列表")
    @GetMapping
    public ApiResponse<List<SysUser>> list() {
        List<SysUser> users = sysUserMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .orderByDesc(SysUser::getCreatedAt)
        );
        // 脱敏：不返回密码
        users.forEach(u -> u.setPassword(null));
        return ApiResponse.success(users);
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public ApiResponse<Void> create(@RequestBody CreateUserRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ApiResponse.error("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ApiResponse.error("密码至少 6 位");
        }

        // 检查用户名是否已存在
        Long count = sysUserMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername())
        );
        if (count != null && count > 0) {
            return ApiResponse.error("用户名已存在");
        }

        SysUser user = SysUser.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .realName(request.getRealName())
                .role(request.getRole() != null ? request.getRole() : "VIEWER")
                .phone(request.getPhone())
                .status(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sysUserMapper.insert(user);
        saveUserLog("CREATE_USER", "创建用户：" + user.getUsername() + "，角色：" + user.getRole(), null, user);
        return ApiResponse.success(null, "用户创建成功");
    }

    @Operation(summary = "修改用户基础信息")
    @PutMapping("/{id}")
    public ApiResponse<SysUser> update(@PathVariable Integer id, @RequestBody UpdateUserRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }

        String realName = trimToNull(request.getRealName());
        String phone = request.getPhone() == null ? null : request.getPhone().trim();
        if (realName == null) {
            return ApiResponse.error("姓名不能为空");
        }
        if (phone != null && !phone.isEmpty() && !phone.matches("^1\\d{10}$")) {
            return ApiResponse.error("请输入正确的 11 位手机号");
        }

        SysUser update = new SysUser();
        update.setId(id);
        update.setRealName(realName);
        update.setPhone(phone);
        update.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(update);

        SysUser updatedUser = sysUserMapper.selectById(id);
        updatedUser.setPassword(null);
        saveUserLog("UPDATE_USER", "修改用户 " + user.getUsername() + " 的基础信息", user, updatedUser);
        return ApiResponse.success(updatedUser, "用户信息更新成功");
    }

    @Operation(summary = "修改用户状态")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Integer id, @RequestBody StatusRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        if (request.getStatus() == null || (request.getStatus() != 0 && request.getStatus() != 1)) {
            return ApiResponse.error("用户状态不正确");
        }
        if (request.getStatus() == 0 && user.getUsername().equals(SecurityUtils.getCurrentUsername())) {
            return ApiResponse.error("不能禁用当前登录账号");
        }
        // 禁止禁用最后一个管理员
        if (request.getStatus() == 0 && isLastEnabledAdmin(user)) {
            return ApiResponse.error("至少保留一个启用状态的管理员");
        }

        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(request.getStatus());
        update.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(update);
        saveUserLog("UPDATE_USER_STATUS",
                "修改用户 " + user.getUsername() + " 状态为 " + (request.getStatus() == 1 ? "启用" : "禁用"),
                user, update);
        return ApiResponse.success(null, "状态更新成功");
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Integer id, @RequestBody PasswordRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ApiResponse.error("密码至少 6 位");
        }

        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(request.getPassword()));
        update.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(update);
        saveUserLog("RESET_USER_PASSWORD",
                "重置用户 " + user.getUsername() + " 的密码",
                user, "{\"reset\":true}");
        return ApiResponse.success(null, "密码重置成功");
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        if (user.getUsername().equals(SecurityUtils.getCurrentUsername())) {
            return ApiResponse.error("不能删除当前登录账号");
        }
        if (isLastEnabledAdmin(user)) {
            return ApiResponse.error("至少保留一个启用状态的管理员");
        }

        sysUserMapper.deleteById(id);
        saveUserLog("DELETE_USER", "删除用户：" + user.getUsername(), user, "{\"deleted\":true}");
        return ApiResponse.success(null, "用户删除成功");
    }

    private boolean isLastEnabledAdmin(SysUser user) {
        if (!"ADMIN".equals(user.getRole()) || user.getStatus() == null || user.getStatus() != 1) {
            return false;
        }
        Long adminCount = sysUserMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "ADMIN")
                        .eq(SysUser::getStatus, 1)
        );
        return adminCount != null && adminCount <= 1;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void saveUserLog(String action, String description, Object before, Object after) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            SysUser operator = username != null ? sysUserMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUsername, username)
            ) : null;

            OperationLog operationLog = OperationLog.builder()
                    .userId(operator != null ? operator.getId() : null)
                    .userName(username != null ? username : "system")
                    .action(action)
                    .description(description)
                    .beforeData(before != null ? maskAndSerialize(before) : null)
                    .afterData(after != null ? maskAndSerialize(after) : null)
                    .ipAddress(SecurityUtils.getCurrentIpAddress())
                    .build();
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            // 日志失败不影响主业务
        }
    }

    private String maskAndSerialize(Object obj) throws Exception {
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof SysUser) {
            SysUser copy = new SysUser();
            org.springframework.beans.BeanUtils.copyProperties(obj, copy);
            copy.setPassword(null);
            return objectMapper.writeValueAsString(copy);
        }
        return objectMapper.writeValueAsString(obj);
    }

    @Data
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String realName;
        private String role;
        private String phone;
    }

    @Data
    public static class UpdateUserRequest {
        private String realName;
        private String phone;
    }

    @Data
    public static class StatusRequest {
        private Integer status;
    }

    @Data
    public static class PasswordRequest {
        private String password;
    }
}

