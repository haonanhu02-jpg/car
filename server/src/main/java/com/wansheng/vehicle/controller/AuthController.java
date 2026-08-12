package com.wansheng.vehicle.controller;

import com.wansheng.vehicle.dto.ApiResponse;
import com.wansheng.vehicle.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证 API
 */
@Tag(name = "认证", description = "登录与 Token 管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final com.wansheng.vehicle.repository.SysUserMapper sysUserMapper;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody LoginRequest request) {
        String realName = request.getRealName() == null ? null : request.getRealName().trim();
        var user = realName == null ? null : sysUserMapper.findByRealName(realName);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ApiResponse.error("姓名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole());

        return ApiResponse.success(Map.of(
                "token", token,
                "username", user.getUsername(),
                "realName", user.getRealName(),
                "role", user.getRole()
        ), "登录成功");
    }

    @Data
    static class LoginRequest {
        private String realName;
        private String password;
    }
}
