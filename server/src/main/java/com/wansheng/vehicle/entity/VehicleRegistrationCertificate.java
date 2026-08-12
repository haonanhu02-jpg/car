package com.wansheng.vehicle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 每台车辆唯一的一份车辆登记证扫描件。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("vehicle_registration_certificate")
public class VehicleRegistrationCertificate {

    @TableId(value = "vehicle_id", type = IdType.INPUT)
    private Integer vehicleId;

    private String fileName;
    private String contentType;
    private Long fileSize;
    private byte[] fileData;
    private LocalDateTime updatedAt;
}
