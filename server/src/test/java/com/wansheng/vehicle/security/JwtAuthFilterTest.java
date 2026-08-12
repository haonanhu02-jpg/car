package com.wansheng.vehicle.security;

import com.wansheng.vehicle.entity.SysUser;
import com.wansheng.vehicle.repository.SysUserMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsTokenWhenUserWasDeletedOrDisabled() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtils, sysUserMapper);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = requestWithToken();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("deleted-user");
        when(sysUserMapper.findByUsername("deleted-user")).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("用户不存在或已被禁用");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void usesCurrentDatabaseRoleForAuthorizedUser() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtils, sysUserMapper);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = requestWithToken();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("viewer");
        when(sysUserMapper.findByUsername("viewer"))
                .thenReturn(SysUser.builder().username("viewer").role("VIEWER").status(1).build());

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_VIEWER");
        verify(chain).doFilter(request, response);
    }

    private MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboard/statistics");
        request.addHeader("Authorization", "Bearer valid-token");
        return request;
    }
}

