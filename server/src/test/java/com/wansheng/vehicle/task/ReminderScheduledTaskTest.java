package com.wansheng.vehicle.task;

import com.wansheng.vehicle.entity.ReminderConfig;
import com.wansheng.vehicle.entity.SystemConfig;
import com.wansheng.vehicle.entity.Vehicle;
import com.wansheng.vehicle.repository.OperationLogMapper;
import com.wansheng.vehicle.repository.ReminderConfigMapper;
import com.wansheng.vehicle.repository.ReminderMapper;
import com.wansheng.vehicle.repository.SystemConfigMapper;
import com.wansheng.vehicle.repository.VehicleMapper;
import com.wansheng.vehicle.service.MailService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReminderScheduledTaskTest {

    @Test
    void usesMethodsFromTheMatchingReminderNode() {
        VehicleMapper vehicleMapper = mock(VehicleMapper.class);
        ReminderMapper reminderMapper = mock(ReminderMapper.class);
        ReminderConfigMapper configMapper = mock(ReminderConfigMapper.class);
        OperationLogMapper logMapper = mock(OperationLogMapper.class);
        MailService mailService = mock(MailService.class);
        SystemConfigMapper systemConfigMapper = mock(SystemConfigMapper.class);
        ReminderScheduledTask task = new ReminderScheduledTask(
                vehicleMapper, reminderMapper, configMapper, logMapper, mailService, systemConfigMapper);

        ReminderConfig thirtyDays = ReminderConfig.builder()
                .type(0).nodeDays(30).enabled(1).remindMethods("system").build();
        ReminderConfig sevenDays = ReminderConfig.builder()
                .type(0).nodeDays(7).enabled(1).remindMethods("system,email").build();
        ReminderConfig inspection = ReminderConfig.builder()
                .type(1).nodeDays(30).enabled(1).remindMethods("system").build();
        when(configMapper.findEnabledByType(0)).thenReturn(List.of(thirtyDays, sevenDays));
        when(configMapper.findEnabledByType(1)).thenReturn(List.of(inspection));

        Vehicle vehicle = Vehicle.builder()
                .id(1).plateNumber("浙J.U0055")
                .insuranceExpire(LocalDate.now().plusDays(7)).status(1).build();
        when(vehicleMapper.selectList(any()))
                .thenReturn(Collections.emptyList(), List.of(vehicle), Collections.emptyList());
        when(reminderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(reminderMapper.findPendingReminders()).thenReturn(Collections.emptyList());
        when(systemConfigMapper.selectOne(any())).thenReturn(
                config("notify_email", "zhongzhenggen@ws-chem.com"));
        when(mailService.sendReminder(anyString(), anyString(), anyString(), anyInt())).thenReturn(true);

        task.scanExpiringVehicles();

        verify(mailService).sendReminder("zhongzhenggen@ws-chem.com", "浙J.U0055", "保险", 7);
        verify(logMapper).insert(argThat(log -> "EMAIL_REMINDER".equals(log.getAction())));
    }

    private SystemConfig config(String key, String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }
}
