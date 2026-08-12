package com.wansheng.vehicle.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wansheng.vehicle.dto.ApiResponse;
import com.wansheng.vehicle.entity.SysUser;
import com.wansheng.vehicle.repository.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 *
 * 每次请求时从 Header 提取 Token，验证并设置安全上下文。
 * 如果 Token 无效或过期，返回 401 而非静默放行。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            if (jwtUtils.validateToken(token)) {
                String username = jwtUtils.getUsernameFromToken(token);
                SysUser user = sysUserMapper.findByUsername(username);
                if (user == null) {
                    writeUnauthorized(response, "用户不存在或已被禁用，请重新登录");
                    return;
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                username, null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                // Token 无效或过期 — 返回 401，而不是静默放行
                writeUnauthorized(response, "Token 无效或已过期，请重新登录");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message));
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}

