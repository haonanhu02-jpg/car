package com.wansheng.vehicle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wansheng.vehicle.dto.*;
import com.wansheng.vehicle.entity.InsuranceHistory;
import com.wansheng.vehicle.entity.InspectionHistory;
import com.wansheng.vehicle.entity.Vehicle;
import com.wansheng.vehicle.entity.VehicleRegistrationCertificate;
import com.wansheng.vehicle.service.VehicleCertificateService;
import com.wansheng.vehicle.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 车辆管理 API
 *
 * 🎯 薄控制器原则：只做参数校验 → 调用 Service → 返回响应
 */
@Tag(name = "车辆管理", description = "车辆台账的增删改查")
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleCertificateService certificateService;

    // ══════════════════════════════════════
    //  查询接口
    // ══════════════════════════════════════

    @Operation(summary = "分页查询车辆列表")
    @GetMapping
    public ApiResponse<PageResult<Vehicle>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer vehicleType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {

        Page<Vehicle> result = vehicleService.listVehicles(keyword, vehicleType, page, size);
        PageResult<Vehicle> pageResult = PageResult.of(
                result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize()
        );
        return ApiResponse.success(pageResult);
    }

    @Operation(summary = "获取车辆详情")
    @GetMapping("/{id}")
    public ApiResponse<Vehicle> detail(@PathVariable Integer id) {
        return ApiResponse.success(vehicleService.getDetail(id));
    }

    @Operation(summary = "获取车辆登记证信息")
    @GetMapping("/{id}/registration-certificate/info")
    public ApiResponse<VehicleCertificateInfo> registrationCertificateInfo(@PathVariable Integer id) {
        return ApiResponse.success(certificateService.getInfo(id));
    }

    @Operation(summary = "查看车辆登记证扫描件")
    @GetMapping("/{id}/registration-certificate")
    public ResponseEntity<byte[]> registrationCertificate(@PathVariable Integer id) {
        VehicleRegistrationCertificate certificate = certificateService.getFile(id);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(certificate.getContentType());
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(certificate.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(certificate.getFileSize())
                .body(certificate.getFileData());
    }

    // ══════════════════════════════════════
    //  命令接口（仅管理员）
    // ══════════════════════════════════════

    @Operation(summary = "新增车辆")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Vehicle> create(@Valid @RequestBody VehicleDTO dto) {
        return ApiResponse.success(vehicleService.create(dto), "车辆注册成功");
    }

    @Operation(summary = "批量新增车辆（JSON 数组）")
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> batchCreate(@Valid @RequestBody VehicleBatchDTO batch) {
        return ApiResponse.success(vehicleService.batchCreate(batch.getItems()), "批量导入成功");
    }

    @Operation(summary = "Excel 批量导入车辆（按车牌号去重，存在则更新）")
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            ImportResult result = vehicleService.importFromExcel(file);
            return ApiResponse.success(result,
                    "导入成功：新增 " + result.getInserted() + " 条，更新 " + result.getUpdated() + " 条");
        } catch (Exception e) {
            return ApiResponse.error(500, "导入失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新车辆信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Vehicle> update(@PathVariable Integer id, @Valid @RequestBody VehicleDTO dto) {
        return ApiResponse.success(vehicleService.update(id, dto), "更新成功");
    }

    @Operation(summary = "上传或替换车辆登记证扫描件")
    @PostMapping("/{id}/registration-certificate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VehicleCertificateInfo> uploadRegistrationCertificate(
            @PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(certificateService.upload(id, file), "车辆登记证上传成功");
    }

    @Operation(summary = "删除车辆登记证扫描件")
    @DeleteMapping("/{id}/registration-certificate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteRegistrationCertificate(@PathVariable Integer id) {
        certificateService.delete(id);
        return ApiResponse.success(null, "车辆登记证已删除");
    }

    @Operation(summary = "删除车辆（软删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        vehicleService.delete(id);
        return ApiResponse.success(null, "车辆已注销");
    }

    @Operation(summary = "续保 — 旧保单归档，新保单写入")
    @PostMapping("/{id}/renew-insurance")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InsuranceHistory> renewInsurance(
            @PathVariable Integer id,
            @RequestBody RenewInsuranceDTO dto) {
        return ApiResponse.success(
                vehicleService.renewInsurance(id, dto, null),
                "续保成功"
        );
    }

    @Operation(summary = "更新年检")
    @PostMapping("/{id}/update-inspection")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InspectionHistory> updateInspection(
            @PathVariable Integer id,
            @RequestParam java.time.LocalDate inspectionDate,
            @RequestParam java.time.LocalDate expireDate) {
        return ApiResponse.success(
                vehicleService.updateInspection(id, inspectionDate, expireDate, null),
                "年检更新成功"
        );
    }
}
