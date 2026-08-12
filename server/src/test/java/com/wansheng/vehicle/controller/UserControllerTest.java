package com.wansheng.vehicle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wansheng.vehicle.entity.SysUser;
import com.wansheng.vehicle.repository.OperationLogMapper;
import com.wansheng.vehicle.repository.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private SysUserMapper sysUserMapper;
    private UserController controller;

    @BeforeEach
    void setUp() {
        sysUserMapper = mock(SysUserMapper.class);
        controller = new UserController(
                sysUserMapper,
                mock(OperationLogMapper.class),
                mock(PasswordEncoder.class),
                new ObjectMapper()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updatesViewerBasicInformation() {
        SysUser viewer = user(2, "viewer", "李四", "VIEWER", "13800000002", 1);
        SysUser updated = user(2, "viewer", "王五", "VIEWER", "13900000003", 1);
        when(sysUserMapper.selectById(2)).thenReturn(viewer, updated);

        UserController.UpdateUserRequest request = new UserController.UpdateUserRequest();
        request.setRealName(" 王五 ");
        request.setPhone("13900000003");

        var response = controller.update(2, request);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData().getRealName()).isEqualTo("王五");
        assertThat(response.getData().getPassword()).isNull();
        ArgumentCaptor<SysUser> updateCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getRealName()).isEqualTo("王五");
        assertThat(updateCaptor.getValue().getPhone()).isEqualTo("13900000003");
    }

    @Test
    void updatesCurrentAdministratorNameWithoutChangingRole() {
        SysUser admin = user(1, "admin", "张姐", "ADMIN", "13800000001", 1);
        SysUser updated = user(1, "admin", "张经理", "ADMIN", "13800000001", 1);
        when(sysUserMapper.selectById(1)).thenReturn(admin, updated);

        UserController.UpdateUserRequest request = new UserController.UpdateUserRequest();
        request.setRealName("张经理");
        request.setPhone("13800000001");

        var response = controller.update(1, request);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData().getRealName()).isEqualTo("张经理");
    }

    @Test
    void deletesAnotherUser() {
        SysUser viewer = user(2, "viewer", "李四", "VIEWER", "13800000002", 1);
        when(sysUserMapper.selectById(2)).thenReturn(viewer);
        when(sysUserMapper.deleteById(2)).thenReturn(1);

        var response = controller.delete(2);

        assertThat(response.getCode()).isZero();
        verify(sysUserMapper).deleteById(2);
    }

    @Test
    void refusesToDeleteCurrentUser() {
        when(sysUserMapper.selectById(1))
                .thenReturn(user(1, "admin", "张姐", "ADMIN", "13800000001", 1));

        var response = controller.delete(1);

        assertThat(response.getCode()).isEqualTo(1);
        assertThat(response.getMessage()).contains("当前登录账号");
        verify(sysUserMapper, never()).deleteById(anyInt());
    }

    @Test
    void refusesToDeleteLastEnabledAdministrator() {
        when(sysUserMapper.selectById(3))
                .thenReturn(user(3, "other-admin", "管理员", "ADMIN", "13800000009", 1));
        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        var response = controller.delete(3);

        assertThat(response.getCode()).isEqualTo(1);
        assertThat(response.getMessage()).contains("至少保留一个");
        verify(sysUserMapper, never()).deleteById(anyInt());
    }

    private SysUser user(Integer id, String username, String realName, String role, String phone, Integer status) {
        return SysUser.builder()
                .id(id)
                .username(username)
                .password("encoded")
                .realName(realName)
                .role(role)
                .phone(phone)
                .status(status)
                .build();
    }
}

