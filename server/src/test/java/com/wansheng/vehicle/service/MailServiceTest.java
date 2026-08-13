package com.wansheng.vehicle.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MailServiceTest {

    private JavaMailSender mailSender;
    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        mailService = new MailService(mailSender);
        ReflectionTestUtils.setField(mailService, "from", "zhongzhenggen@ws-chem.com");
    }

    @Test
    void reportsSuccessfulReminderDelivery() {
        boolean sent = mailService.sendReminder(
                "zhongzhenggen@ws-chem.com", "浙J.U0055", "保险", 7);

        assertThat(sent).isTrue();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void reportsFailedReminderDelivery() {
        doThrow(new RuntimeException("SMTP authentication failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        boolean sent = mailService.sendReminder(
                "zhongzhenggen@ws-chem.com", "浙J.U0055", "保险", 7);

        assertThat(sent).isFalse();
    }
}
