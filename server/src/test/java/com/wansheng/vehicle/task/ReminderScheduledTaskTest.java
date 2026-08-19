package com.wansheng.vehicle.task;

import com.wansheng.vehicle.entity.Reminder;
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
import static org.assertj.core.api.Assertions.assertThat;

class ReminderScheduledTaskTest {

    @Test
    void repeatedScanOnTheSameNodeDoesNotSendDuplicateNotification() {
        VehicleMapper vehicleMapper = mock(VehicleMapper.class);
        ReminderMapper reminderMapper = mock(ReminderMapper.class);
        ReminderConfigMapper configMapper = mock(ReminderConfigMapper.class);
        MailService mailService = mock(MailService.class);
        ReminderScheduledTask task = new ReminderScheduledTask(
                vehicleMapper,
                reminderMapper,
                configMapper,
                mock(OperationLogMapper.class),
                mailService,
                mock(SystemConfigMapper.class));

        LocalDate today = LocalDate.now();
        LocalDate expiry = today.plusDays(7);
        Vehicle vehicle = Vehicle.builder()
                .id(6).plateNumber("浙J.SAME6")
                .insuranceExpire(expiry).status(1).build();
        Reminder existing = Reminder.builder()
                .id(66).vehicleId(6).type(0).nodeDays(7)
                .remindDate(today).expireDate(expiry)
                .status(0).archived(0).build();
        when(configMapper.findEnabledByType(0)).thenReturn(List.of(
                ReminderConfig.builder().type(0).nodeDays(7).enabled(1)
                        .remindMethods("system,email").build()));
        when(configMapper.findEnabledByType(1)).thenReturn(List.of(
                ReminderConfig.builder().type(1).nodeDays(7).enabled(1)
                        .remindMethods("system,email").build()));
        when(reminderMapper.selectList(any())).thenReturn(List.of(existing));
        when(reminderMapper.findPendingReminders()).thenReturn(List.of(existing));
        when(reminderMapper.findByCycle(6, 0, expiry)).thenReturn(existing);
        when(vehicleMapper.selectById(6)).thenReturn(vehicle);
        when(vehicleMapper.selectList(any()))
                .thenReturn(List.of(vehicle), Collections.emptyList());

        task.scanExpiringVehicles();

        verify(reminderMapper, never()).insert(any());
        verify(mailService, never()).sendReminder(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void laterNodeUpdatesTheSameReminderCycleInsteadOfCreatingAnotherRow() {
        VehicleMapper vehicleMapper = mock(VehicleMapper.class);
        ReminderMapper reminderMapper = mock(ReminderMapper.class);
        ReminderConfigMapper configMapper = mock(ReminderConfigMapper.class);
        OperationLogMapper logMapper = mock(OperationLogMapper.class);
        MailService mailService = mock(MailService.class);
        SystemConfigMapper systemConfigMapper = mock(SystemConfigMapper.class);
        ReminderScheduledTask task = new ReminderScheduledTask(
                vehicleMapper, reminderMapper, configMapper, logMapper, mailService, systemConfigMapper);

        LocalDate today = LocalDate.now();
        LocalDate expiry = today.plusDays(15);
        Vehicle vehicle = Vehicle.builder()
                .id(8).plateNumber("浙J.TEST8")
                .insuranceExpire(expiry).status(1).build();
        Reminder existingCycle = Reminder.builder()
                .id(88).vehicleId(8).type(0).nodeDays(30)
                .remindDate(expiry.minusDays(30))
                .remindMethod("system,email").status(2).build();
        ReminderConfig insuranceNode = ReminderConfig.builder()
                .type(0).nodeDays(15).enabled(1).remindMethods("system,email").build();
        ReminderConfig inspectionNode = ReminderConfig.builder()
                .type(1).nodeDays(15).enabled(1).remindMethods("system,email").build();

        when(configMapper.findEnabledByType(0)).thenReturn(List.of(insuranceNode));
        when(configMapper.findEnabledByType(1)).thenReturn(List.of(inspectionNode));
        when(reminderMapper.selectList(any())).thenReturn(List.of(existingCycle));
        when(reminderMapper.findByCycle(8, 0, expiry)).thenReturn(existingCycle);
        when(vehicleMapper.selectById(8)).thenReturn(vehicle);
        when(vehicleMapper.selectList(any()))
                .thenReturn(List.of(vehicle), Collections.emptyList());
        when(reminderMapper.findPendingReminders()).thenReturn(Collections.emptyList());
        when(systemConfigMapper.selectOne(any())).thenReturn(
                config("notify_email", "zhongzhenggen@ws-chem.com"));
        when(mailService.sendReminder(anyString(), anyString(), anyString(), anyInt())).thenReturn(true);

        task.scanExpiringVehicles();

        verify(reminderMapper, never()).insert(any());
        verify(reminderMapper).updateById(argThat(reminder -> {
            assertThat(reminder.getId()).isEqualTo(88);
            assertThat(reminder.getNodeDays()).isEqualTo(15);
            assertThat(reminder.getRemindDate()).isEqualTo(today);
            assertThat(reminder.getStatus()).isEqualTo(2);
            return true;
        }));
        verify(mailService).sendReminder("zhongzhenggen@ws-chem.com", "浙J.TEST8", "保险", 15);
    }

    @Test
    void removesUnresolvedReminderWhenVehicleExpiryDateHasChanged() {
        VehicleMapper vehicleMapper = mock(VehicleMapper.class);
        ReminderMapper reminderMapper = mock(ReminderMapper.class);
        ReminderConfigMapper configMapper = mock(ReminderConfigMapper.class);
        OperationLogMapper logMapper = mock(OperationLogMapper.class);
        MailService mailService = mock(MailService.class);
        SystemConfigMapper systemConfigMapper = mock(SystemConfigMapper.class);
        ReminderScheduledTask task = new ReminderScheduledTask(
                vehicleMapper, reminderMapper, configMapper, logMapper, mailService, systemConfigMapper);

        Reminder staleReminder = Reminder.builder()
                .id(1)
                .vehicleId(2)
                .type(1)
                .nodeDays(7)
                .remindDate(LocalDate.of(2026, 6, 23))
                .status(2)
                .build();
        Vehicle vehicleWithNewExpiry = Vehicle.builder()
                .id(2)
                .plateNumber("浙J.5632U")
                .inspectionExpire(LocalDate.of(2027, 6, 30))
                .status(1)
                .build();

        when(reminderMapper.selectList(any())).thenReturn(List.of(staleReminder));
        when(vehicleMapper.selectById(2)).thenReturn(vehicleWithNewExpiry);
        when(vehicleMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(configMapper.findEnabledByType(anyInt())).thenReturn(Collections.emptyList());
        when(reminderMapper.findPendingReminders()).thenReturn(Collections.emptyList());

        task.scanExpiringVehicles();

        verify(reminderMapper).deleteById(1);
    }

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
