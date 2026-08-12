package com.wansheng.vehicle.service;

import com.wansheng.vehicle.entity.Vehicle;
import com.wansheng.vehicle.entity.VehicleRegistrationCertificate;
import com.wansheng.vehicle.repository.VehicleMapper;
import com.wansheng.vehicle.repository.VehicleRegistrationCertificateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class VehicleCertificateServiceTest {

    private VehicleMapper vehicleMapper;
    private VehicleRegistrationCertificateMapper certificateMapper;
    private VehicleCertificateService service;

    @BeforeEach
    void setUp() {
        vehicleMapper = mock(VehicleMapper.class);
        certificateMapper = mock(VehicleRegistrationCertificateMapper.class);
        service = new VehicleCertificateService(vehicleMapper, certificateMapper);
        when(vehicleMapper.selectById(1)).thenReturn(Vehicle.builder().id(1).plateNumber("浙J.U0055").build());
    }

    @Test
    void uploadsPdfIntoDatabase() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "登记证.pdf", "application/pdf", "pdf-data".getBytes());

        var info = service.upload(1, file);

        assertThat(info.getFileName()).isEqualTo("登记证.pdf");
        ArgumentCaptor<VehicleRegistrationCertificate> captor =
                ArgumentCaptor.forClass(VehicleRegistrationCertificate.class);
        verify(certificateMapper).insert(captor.capture());
        assertThat(captor.getValue().getFileData()).isEqualTo("pdf-data".getBytes());
    }

    @Test
    void rejectsUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.html", "text/html", "bad".getBytes());

        assertThatThrownBy(() -> service.upload(1, file))
                .hasMessageContaining("仅支持图片或 PDF");
        verify(certificateMapper, never()).insert(any());
    }
}
