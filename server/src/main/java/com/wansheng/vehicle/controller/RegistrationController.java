package com.wansheng.vehicle.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wansheng.vehicle.dto.ApiResponse;
import com.wansheng.vehicle.dto.RegisterRequest;
import com.wansheng.vehicle.entity.OperationLog;
import com.wansheng.vehicle.entity.SysUser;
import com.wansheng.vehicle.entity.UserRegistration;
import com.wansheng.vehicle.enums.RegistrationStatus;
import com.wansheng.vehicle.repository.OperationLogMapper;
import com.wansheng.vehicle.repository.SysUserMapper;
import com.wansheng.vehicle.repository.UserRegistrationMapper;
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
import java.util.UUID;

/**
 * 账号注册审批 API
 *
 * 普通用户通过 {@code POST /api/registrations} 自助提交申请（公开接口）；
 * 管理员在「系统设置 → 账号审批」中查看并审批，审批通过后自动生成 sys_user 账号。
 */
@Tag(name = "账号注册审批", description = "用户自助提交注册申请，管理员审批通过后方可登录")
@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserRegistrationMapper regMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    @Operation(summary = "提交注册申请（公开）")
    @PostMapping
    public ApiResponse<Void> apply(@RequestBody RegisterRequest req) {
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            return ApiResponse.error("密码至少 6 位");
        }
        String validationError = validateBasicInfo(
                req.getRealName(), req.getEmployeeNo(), req.getDepartment(), req.getPhone());
        if (validationError != null) {
            return ApiResponse.error(validationError);
        }
        String realName = req.getRealName().trim();

        // 姓名就是登录名，正式用户和待处理申请中都必须保持唯一。
        Long existingUsers = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRealName, realName));
        if (existingUsers != null && existingUsers > 0) {
            return ApiResponse.error("该姓名已存在，请联系管理员");
        }

        Long duplicateApplications = regMapper.selectCount(
                new LambdaQueryWrapper<UserRegistration>()
                        .eq(UserRegistration::getRealName, realName)
                        .in(UserRegistration::getStatus,
                                RegistrationStatus.PENDING.getCode(),
                                RegistrationStatus.APPROVED.getCode()));
        if (duplicateApplications != null && duplicateApplications > 0) {
            return ApiResponse.error("该姓名已有注册申请，请勿重复提交");
        }

        UserRegistration reg = UserRegistration.builder()
                .username(generateInternalUsername("pending_"))
                .password(passwordEncoder.encode(req.getPassword()))
                .realName(realName)
                .employeeNo(req.getEmployeeNo().trim())
                .department(req.getDepartment().trim())
                .phone(req.getPhone().trim())
                .status(RegistrationStatus.PENDING.getCode())
                .build();
        regMapper.insert(reg);

        return ApiResponse.success(null, "申请已提交，请等待管理员审批");
    }

    @Operation(summary = "申请列表（管理员）")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserRegistration>> list(@RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<UserRegistration> wrapper = new LambdaQueryWrapper<UserRegistration>()
                .orderByDesc(UserRegistration::getCreatedAt);
        if (status != null) {
            wrapper.eq(UserRegistration::getStatus, status);
        }
        List<UserRegistration> list = regMapper.selectList(wrapper);
        // 脱敏：不返回密码
        list.forEach(r -> r.setPassword(null));
        return ApiResponse.success(list);
    }

    @Operation(summary = "修改待审批申请信息（管理员）")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserRegistration> update(@PathVariable Integer id,
                                                @RequestBody UpdateRegistrationRequest req) {
        UserRegistration reg = regMapper.selectById(id);
        if (reg == null) {
            return ApiResponse.error("申请不存在");
        }
        if (!RegistrationStatus.isPending(reg.getStatus())) {
            return ApiResponse.error("仅待审批申请可以编辑");
        }
        String validationError = validateBasicInfo(
                req.getRealName(), req.getEmployeeNo(), req.getDepartment(), req.getPhone());
        if (validationError != null) {
            return ApiResponse.error(validationError);
        }

        String realName = req.getRealName().trim();
        Long existingUsers = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRealName, realName));
        Long duplicateApplications = regMapper.selectCount(
                new LambdaQueryWrapper<UserRegistration>()
                        .eq(UserRegistration::getRealName, realName)
                        .ne(UserRegistration::getId, id)
                        .in(UserRegistration::getStatus,
                                RegistrationStatus.PENDING.getCode(),
                                RegistrationStatus.APPROVED.getCode()));
        if ((existingUsers != null && existingUsers > 0)
                || (duplicateApplications != null && duplicateApplications > 0)) {
            return ApiResponse.error("该姓名已存在");
        }

        reg.setRealName(realName);
        reg.setEmployeeNo(req.getEmployeeNo().trim());
        reg.setDepartment(req.getDepartment().trim());
        reg.setPhone(req.getPhone().trim());
        reg.setUpdatedAt(LocalDateTime.now());
        regMapper.updateById(reg);
        reg.setPassword(null);
        saveLog("UPDATE_REGISTRATION", "修改注册申请：" + realName, null, reg);
        return ApiResponse.success(reg, "申请信息更新成功");
    }

    @Operation(summary = "通过申请（管理员）")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> approve(@PathVariable Integer id) {
        UserRegistration reg = regMapper.selectById(id);
        if (reg == null) {
            return ApiResponse.error("申请不存在");
        }
        if (!RegistrationStatus.isPending(reg.getStatus())) {
            return ApiResponse.error("该申请已处理，不能重复审批");
        }

        // 二次校验：审批时姓名仍未被占用，否则自动拒绝避免重名登录。
        Long existingUsers = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRealName, reg.getRealName()));
        if (existingUsers != null && existingUsers > 0) {
            reg.setStatus(RegistrationStatus.REJECTED.getCode());
            reg.setRejectReason("审批时姓名已被占用");
            reg.setReviewerId(currentUserId());
            reg.setReviewedAt(LocalDateTime.now());
            regMapper.updateById(reg);
            return ApiResponse.error("姓名已被占用，已自动拒绝该申请");
        }

        SysUser user = SysUser.builder()
                .username(reg.getUsername())
                .password(reg.getPassword()) // 申请时已 BCrypt 加密
                .realName(reg.getRealName())
                .role("VIEWER")
                .phone(reg.getPhone())
                .status(1)
                .build();
        sysUserMapper.insert(user);

        reg.setStatus(RegistrationStatus.APPROVED.getCode());
        reg.setReviewerId(currentUserId());
        reg.setReviewedAt(LocalDateTime.now());
        regMapper.updateById(reg);

        saveLog("APPROVE_REGISTRATION",
                "审批通过注册申请：" + reg.getRealName() + "（" + reg.getDepartment() + "）", null, null);
        return ApiResponse.success(null, "已通过，用户可登录");
    }

    @Operation(summary = "拒绝申请（管理员）")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> reject(@PathVariable Integer id, @RequestBody RejectRequest req) {
        UserRegistration reg = regMapper.selectById(id);
        if (reg == null) {
            return ApiResponse.error("申请不存在");
        }
        if (!RegistrationStatus.isPending(reg.getStatus())) {
            return ApiResponse.error("该申请已处理，不能重复审批");
        }
        reg.setStatus(RegistrationStatus.REJECTED.getCode());
        reg.setRejectReason(req.getReason());
        reg.setReviewerId(currentUserId());
        reg.setReviewedAt(LocalDateTime.now());
        regMapper.updateById(reg);

        saveLog("REJECT_REGISTRATION",
                "拒绝注册申请：" + reg.getRealName() + "，原因：" + (req.getReason() == null ? "无" : req.getReason()),
                null, null);
        return ApiResponse.success(null, "已拒绝");
    }

    private Integer currentUserId() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return null;
        }
        SysUser u = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        return u != null ? u.getId() : null;
    }

    private void saveLog(String action, String description, Object before, Object after) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            OperationLog log = OperationLog.builder()
                    .userId(currentUserId())
                    .userName(username != null ? username : "system")
                    .action(action)
                    .description(description)
                    .beforeData(before != null ? objectMapper.writeValueAsString(before) : null)
                    .afterData(after != null ? objectMapper.writeValueAsString(after) : null)
                    .ipAddress(SecurityUtils.getCurrentIpAddress())
                    .build();
            operationLogMapper.insert(log);
        } catch (Exception ignored) {
            // 日志写入失败不影响主流程
        }
    }

    private String validateBasicInfo(String realName, String employeeNo, String department, String phone) {
        if (realName == null || realName.isBlank()) return "姓名不能为空";
        if (employeeNo == null || employeeNo.isBlank()) return "工号不能为空";
        if (department == null || department.isBlank()) return "部门不能为空";
        if (phone == null || phone.isBlank()) return "手机号不能为空";
        if (!phone.trim().matches("^1\\d{10}$")) return "请输入正确的 11 位手机号";
        return null;
    }

    private String generateInternalUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    @Data
    public static class RejectRequest {
        private String reason;
    }

    @Data
    public static class UpdateRegistrationRequest {
        private String realName;
        private String employeeNo;
        private String department;
        private String phone;
    }
}
