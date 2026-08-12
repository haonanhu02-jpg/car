package com.wansheng.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCertificateInfo {
    private String fileName;
    private String contentType;
    private Long fileSize;
    private LocalDateTime updatedAt;
}
