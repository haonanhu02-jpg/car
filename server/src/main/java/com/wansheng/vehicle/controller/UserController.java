package com.wansheng.vehicle.controller;

import com.wansheng.vehicle.dto.ApiResponse;
import com.wansheng.vehicle.entity.SysUser;
import com.wansheng.vehicle.repository.SysUserMapper;
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
    private final PasswordEncoder passwordEncoder;

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
        return ApiResponse.success(null, "用户创建成功");
    }

    @Operation(summary = "修改用户状态")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Integer id, @RequestBody StatusRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        // 禁止禁用最后一个管理员
        if (request.getStatus() != null && request.getStatus() == 0 && "ADMIN".equals(user.getRole())) {
            Long adminCount = sysUserMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getRole, "ADMIN")
                            .eq(SysUser::getStatus, 1)
            );
            if (adminCount != null && adminCount <= 1) {
                return ApiResponse.error("至少保留一个启用状态的管理员");
            }
        }

        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(request.getStatus());
        update.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(update);
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
        return ApiResponse.success(null, "密码重置成功");
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
    public static class StatusRequest {
        private Integer status;
    }

    @Data
    public static class PasswordRequest {
        private String password;
    }
}
