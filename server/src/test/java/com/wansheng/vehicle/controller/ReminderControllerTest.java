package com.wansheng.vehicle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wansheng.vehicle.dto.PageResult;
import com.wansheng.vehicle.entity.Reminder;
import com.wansheng.vehicle.entity.Vehicle;
import com.wansheng.vehicle.repository.ReminderMapper;
import com.wansheng.vehicle.repository.VehicleMapper;
import com.wansheng.vehicle.task.ReminderScheduledTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReminderControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void todoListIsPagedAndIncludesActualExpiryStatus() {
        ReminderMapper reminderMapper = mock(ReminderMapper.class);
        VehicleMapper vehicleMapper = mock(VehicleMapper.class);
        ReminderController controller = new ReminderController(
                reminderMapper, vehicleMapper, mock(ReminderScheduledTask.class));
        LocalDate expiry = LocalDate.now().plusDays(13);
        Reminder reminder = Reminder.builder()
                .id(1).vehicleId(5).type(1).nodeDays(15)
                .remindDate(LocalDate.now().minusDays(2))
                .expireDate(expiry).status(2).archived(0).build();
        when(reminderMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Reminder> result = invocation.getArgument(0);
            result.setRecords(List.of(reminder));
            result.setTotal(1);
            return result;
        });
        when(vehicleMapper.selectBatchIds(any())).thenReturn(
                List.of(Vehicle.builder().id(5).plateNumber("浙J.5T7L5").build()));

        PageResult<Reminder> result = controller.list(
                "todo", null, null, null, null, null, 1, 20).getData();

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).singleElement().satisfies(row -> {
            assertThat(row.getPlateNumber()).isEqualTo("浙J.5T7L5");
            assertThat(row.getRemainingDays()).isEqualTo(13);
            assertThat(row.getExpireStatus()).isEqualTo("剩余 13 天");
            assertThat(row.getStatus()).isEqualTo(2);
        });
    }

    @Test
    void timedOutReminderCanStillBeMarkedHandled() {
        ReminderMapper reminderMapper = mock(ReminderMapper.class);
        ReminderController controller = new ReminderController(
                reminderMapper, mock(VehicleMapper.class), mock(ReminderScheduledTask.class));
        Reminder timedOut = Reminder.builder().id(9).status(2).archived(0).build();
        when(reminderMapper.selectById(9)).thenReturn(timedOut);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null));

        controller.handle(9);

        verify(reminderMapper).updateById(argThat(reminder ->
                reminder.getStatus() == 1
                        && "admin".equals(reminder.getHandler())
                        && reminder.getHandledAt() != null));
    }
}
