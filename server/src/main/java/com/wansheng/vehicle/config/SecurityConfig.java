package com.wansheng.vehicle.config;

import com.wansheng.vehicle.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置
 *
 * 放行登录、静态资源、H2 控制台和 API 文档；
 * 其余所有请求必须携带有效 JWT Token。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
    // 1. 放行登录接口（必须和 AuthController 里的路径一致）
                .requestMatchers("/api/auth/login").permitAll()
    // 1.1 放行自助注册申请提交接口（仅精确匹配 POST /api/registrations，审批类子路径仍需认证+管理员）
                .requestMatchers(HttpMethod.POST, "/api/registrations").permitAll()
    // 2. 彻底消除 favicon 报错的根源：放行根路径和静态资源
                .requestMatchers("/", "/favicon.ico", "/index.html", "/error").permitAll()
    // 3. 放行 H2 控制台和 API 文档（仅联调阶段）
                .requestMatchers("/h2-console/**", "/doc.html", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
    // 4. 除了上面放行的，其他所有请求都必须通过认证（需要 Token）
                 .anyRequest().authenticated()
                )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 明确允许的前端来源（不再使用通配符 * + credentials 组合）
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost",
            "http://127.0.0.1"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
