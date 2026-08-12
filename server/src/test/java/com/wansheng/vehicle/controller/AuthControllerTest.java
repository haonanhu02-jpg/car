package com.wansheng.vehicle.controller;

import com.wansheng.vehicle.entity.SysUser;
import com.wansheng.vehicle.repository.SysUserMapper;
import com.wansheng.vehicle.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Test
    void logsInWithRealNameAndKeepsInternalUsernameInToken() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        SysUserMapper mapper = mock(SysUserMapper.class);
        AuthController controller = new AuthController(jwtUtils, encoder, mapper);
        SysUser admin = SysUser.builder()
                .username("admin")
                .password("encoded")
                .realName("钟正根")
                .role("ADMIN")
                .status(1)
                .build();
        when(mapper.findByRealName("钟正根")).thenReturn(admin);
        when(encoder.matches("admin123", "encoded")).thenReturn(true);
        when(jwtUtils.generateToken("admin", "ADMIN")).thenReturn("token");

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setRealName(" 钟正根 ");
        request.setPassword("admin123");

        var response = controller.login(request);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData().get("realName")).isEqualTo("钟正根");
        assertThat(response.getData().get("token")).isEqualTo("token");
        verify(mapper).findByRealName("钟正根");
        verify(mapper, never()).findByUsername(anyString());
    }
}
