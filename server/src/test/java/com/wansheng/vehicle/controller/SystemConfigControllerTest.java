package com.wansheng.vehicle.controller;

import com.wansheng.vehicle.repository.SystemConfigMapper;
import com.wansheng.vehicle.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SystemConfigControllerTest {

    private SystemConfigMapper mapper;
    private MailService mailService;
    private SystemConfigController controller;

    @BeforeEach
    void setUp() {
        mapper = mock(SystemConfigMapper.class);
        mailService = mock(MailService.class);
        controller = new SystemConfigController(mapper, mailService);
    }

    @Test
    void sendsTestEmailToValidatedAddress() {
        when(mailService.sendTest("zhongzhenggen@ws-chem.com")).thenReturn(true);

        var response = controller.testNotifyEmail(
                java.util.Map.of("email", " zhongzhenggen@ws-chem.com "));

        assertThat(response.getCode()).isZero();
        verify(mailService).sendTest("zhongzhenggen@ws-chem.com");
    }

    @Test
    void rejectsInvalidAddressWithoutSending() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                controller.testNotifyEmail(java.util.Map.of("email", "not-an-email")))
                .hasMessageContaining("正确的邮箱地址");
        verify(mailService, never()).sendTest(any());
    }
}
