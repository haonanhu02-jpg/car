package com.wansheng.vehicle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wansheng.vehicle.dto.VehicleDTO;
import com.wansheng.vehicle.dto.DashboardStats;
import com.wansheng.vehicle.entity.Vehicle;
import com.wansheng.vehicle.repository.InspectionHistoryMapper;
import com.wansheng.vehicle.repository.InsuranceHistoryMapper;
import com.wansheng.vehicle.repository.OperationLogMapper;
import com.wansheng.vehicle.repository.ReminderMapper;
import com.wansheng.vehicle.repository.SysUserMapper;
import com.wansheng.vehicle.repository.VehicleMapper;
import com.wansheng.vehicle.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VehicleServiceImplTest {

    @Test
    void dashboardCountsTodayUnresolvedRemindersInsteadOfTodayExpiringVehicles() {
        VehicleMapper vehicleMapper = mock(VehicleMapper.class);
        ReminderMapper reminderMapper = mock(ReminderMapper.class);
        VehicleServiceImpl service = new VehicleServiceImpl(
                vehicleMapper,
                mock(InsuranceHistoryMapper.class),
                mock(InspectionHistoryMapper.class),
                reminderMapper,
                mock(OperationLogMapper.class),
                mock(SysUserMapper.class),
                new ObjectMapper().findAndRegisterModules()
        );
        LocalDate today = LocalDate.now();
        when(vehicleMapper.selectCount(any())).thenReturn(25L);
        when(vehicleMapper.findTodayExpiring(today)).thenReturn(Collections.emptyList());
        when(vehicleMapper.findExpiringSoon(any(), any())).thenReturn(Collections.emptyList());
        when(vehicleMapper.findOverdue(today)).thenReturn(Collections.emptyList());
        when(reminderMapper.selectCount(any())).thenReturn(2L);

        DashboardStats stats = service.getDashboardStats();

        assertThat(stats.getTodayExpiring()).isEqualTo(2);
        verify(reminderMapper).selectCount(any());
    }

    @Test
    void updatingInspectionExpiryClearsOnlyUnresolvedInspectionReminders() {
        VehicleMapper vehicleMapper = mock(VehicleMapper.class);
        InsuranceHistoryMapper insuranceHistoryMapper = mock(InsuranceHistoryMapper.class);
        InspectionHistoryMapper inspectionHistoryMapper = mock(InspectionHistoryMapper.class);
        ReminderMapper reminderMapper = mock(ReminderMapper.class);
        OperationLogMapper operationLogMapper = mock(OperationLogMapper.class);
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);
        VehicleServiceImpl service = new VehicleServiceImpl(
                vehicleMapper,
                insuranceHistoryMapper,
                inspectionHistoryMapper,
                reminderMapper,
                operationLogMapper,
                sysUserMapper,
                new ObjectMapper().findAndRegisterModules()
        );

        Vehicle existing = Vehicle.builder()
                .id(2)
                .plateNumber("浙J.5632U")
                .vehicleType(0)
                .insuranceExpire(LocalDate.of(2026, 10, 22))
                .inspectionExpire(LocalDate.of(2026, 6, 30))
                .status(1)
                .build();
        when(vehicleMapper.selectById(2)).thenReturn(existing);
        when(reminderMapper.deleteUnresolvedByVehicleAndType(2, 1)).thenReturn(1);

        VehicleDTO dto = new VehicleDTO();
        dto.setPlateNumber("浙J.5632U");
        dto.setVehicleType(0);
        dto.setInsuranceExpire(LocalDate.of(2026, 10, 22));
        dto.setInspectionExpire(LocalDate.of(2027, 6, 30));

        service.update(2, dto);

        verify(reminderMapper).deleteUnresolvedByVehicleAndType(2, 1);
        verify(reminderMapper, never()).deleteUnresolvedByVehicleAndType(2, 0);
    }
}
