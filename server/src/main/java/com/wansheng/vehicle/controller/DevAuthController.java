package com.wansheng.vehicle.controller;

import com.wansheng.vehicle.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 开发调试用认证接口（仅在非 Docker 环境下激活）
 *
 * 生产环境（Docker 部署）不会加载此 Controller，
 * 避免 /api/auth/gen-pass 调试接口暴露到生产。
 */
@Tag(name = "调试", description = "开发辅助接口")
@Profile("!docker")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class DevAuthController {

    private final PasswordEncoder passwordEncoder;

    /**
     * 联调辅助：生成 BCrypt 密码哈希
     */
    @Operation(summary = "生成BCrypt密码（仅开发环境）")
    @GetMapping("/gen-pass")
    public ApiResponse<String> genPass(@RequestParam String raw) {
        return ApiResponse.success(passwordEncoder.encode(raw));
    }
}
