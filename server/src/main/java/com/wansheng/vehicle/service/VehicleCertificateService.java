package com.wansheng.vehicle.service;

import com.wansheng.vehicle.dto.VehicleCertificateInfo;
import com.wansheng.vehicle.entity.VehicleRegistrationCertificate;
import com.wansheng.vehicle.repository.VehicleMapper;
import com.wansheng.vehicle.repository.VehicleRegistrationCertificateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VehicleCertificateService {

    static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final VehicleMapper vehicleMapper;
    private final VehicleRegistrationCertificateMapper certificateMapper;

    public VehicleCertificateInfo getInfo(Integer vehicleId) {
        ensureVehicleExists(vehicleId);
        VehicleRegistrationCertificate certificate = certificateMapper.selectById(vehicleId);
        return certificate == null ? null : toInfo(certificate);
    }

    public VehicleRegistrationCertificate getFile(Integer vehicleId) {
        ensureVehicleExists(vehicleId);
        VehicleRegistrationCertificate certificate = certificateMapper.selectById(vehicleId);
        if (certificate == null) {
            throw new RuntimeException("该车辆尚未上传车辆登记证");
        }
        return certificate;
    }

    public VehicleCertificateInfo upload(Integer vehicleId, MultipartFile file) {
        ensureVehicleExists(vehicleId);
        validate(file);
        try {
            VehicleRegistrationCertificate certificate = VehicleRegistrationCertificate.builder()
                    .vehicleId(vehicleId)
                    .fileName(safeFileName(file.getOriginalFilename()))
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .fileData(file.getBytes())
                    .updatedAt(LocalDateTime.now())
                    .build();
            if (certificateMapper.selectById(vehicleId) == null) {
                certificateMapper.insert(certificate);
            } else {
                certificateMapper.updateById(certificate);
            }
            return toInfo(certificate);
        } catch (IOException e) {
            throw new RuntimeException("读取车辆登记证文件失败");
        }
    }

    public void delete(Integer vehicleId) {
        ensureVehicleExists(vehicleId);
        certificateMapper.deleteById(vehicleId);
    }

    private void ensureVehicleExists(Integer vehicleId) {
        if (vehicleMapper.selectById(vehicleId) == null) {
            throw new RuntimeException("车辆不存在");
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择车辆登记证扫描文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("车辆登记证文件不能超过 10MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        boolean supportedImage = contentType.startsWith("image/") && !"image/svg+xml".equals(contentType);
        if (!supportedImage && !"application/pdf".equals(contentType)) {
            throw new RuntimeException("仅支持图片或 PDF 格式的车辆登记证");
        }
    }

    private String safeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "车辆登记证";
        }
        String normalized = originalFilename.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return fileName.isBlank() ? "车辆登记证" : fileName;
    }

    private VehicleCertificateInfo toInfo(VehicleRegistrationCertificate certificate) {
        return VehicleCertificateInfo.builder()
                .fileName(certificate.getFileName())
                .contentType(certificate.getContentType())
                .fileSize(certificate.getFileSize())
                .updatedAt(certificate.getUpdatedAt())
                .build();
    }
}
