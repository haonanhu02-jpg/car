package com.wansheng.vehicle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wansheng.vehicle.dto.RegisterRequest;
import com.wansheng.vehicle.entity.UserRegistration;
import com.wansheng.vehicle.repository.OperationLogMapper;
import com.wansheng.vehicle.repository.SysUserMapper;
import com.wansheng.vehicle.repository.UserRegistrationMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegistrationControllerTest {

    @Test
    void createsApplicationFromNameWithoutPublicUsername() {
        UserRegistrationMapper registrationMapper = mock(UserRegistrationMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        RegistrationController controller = new RegistrationController(
                registrationMapper, userMapper, encoder,
                mock(OperationLogMapper.class), new ObjectMapper());
        when(encoder.encode("123456")).thenReturn("encoded");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(registrationMapper.selectCount(any())).thenReturn(0L);

        RegisterRequest request = new RegisterRequest();
        request.setRealName("陶君君");
        request.setPassword("123456");
        request.setEmployeeNo("W001");
        request.setDepartment("行政部");
        request.setPhone("18358586908");

        var response = controller.apply(request);

        assertThat(response.getCode()).isZero();
        ArgumentCaptor<UserRegistration> captor = ArgumentCaptor.forClass(UserRegistration.class);
        verify(registrationMapper).insert(captor.capture());
        assertThat(captor.getValue().getRealName()).isEqualTo("陶君君");
        assertThat(captor.getValue().getUsername()).startsWith("pending_");
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
    }
}
