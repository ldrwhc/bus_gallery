package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.IdempotencyService;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final VehicleService vehicleService;
    private final ImageService imageService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireLogin
    @Transactional(timeout = 30)
    public VehicleController.VehicleDetailResponse uploadVehicle(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "payload", required = false) String payloadJson,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        UploadPayload payload = parsePayload(payloadJson);
        log.info("Upload request received: fileName={}, size={}, plate={}",
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : -1,
                payload != null ? payload.getPlateNumber() : "null");
        return idempotencyService.runOnce(
                idempotencyKey,
                Duration.ofMinutes(10),
                () -> handleUpload(file, payload)
        );
    }

    private VehicleController.VehicleDetailResponse handleUpload(MultipartFile file, UploadPayload payload) {
        payload.validate();

        UserSession session = AuthContextHolder.requireUser();
        Image metadata = new Image();
        metadata.setUploadUser(StringUtils.hasText(session.getDisplayName()) ? session.getDisplayName() : session.getUsername());
        metadata.setUploaderId(session.getUserId());
        metadata.setUploaderUsername(session.getUsername());
        metadata.setUploaderDisplayName(session.getDisplayName());

        Image image = imageService.uploadAndSave(file, metadata);
        Vehicle vehicle = payload.toVehicle();
        VehicleConfig config = payload.toVehicleConfig();

        Vehicle saved = vehicleService.create(
                vehicle,
                config,
                Collections.singletonList(image.getId()),
                payload.getBrandId(),
                payload.getBrandName(),
                payload.getModelName(),
                payload.getCompanyName(),
                payload.getRegionProvince(),
                payload.getRegionCity()
        );

        Vehicle detailVehicle = vehicleService.findById(saved.getId());
        VehicleConfig detailConfig = vehicleService.findConfigByVehicleId(saved.getId());
        List<Image> detailImages = imageService.listByVehicle(saved.getId());

        return VehicleController.assembleDetail(detailVehicle, detailConfig, detailImages);
    }

    private UploadPayload parsePayload(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "上传缺少必要字段: payload");
        }
        try {
            return objectMapper.readValue(payloadJson, UploadPayload.class);
        } catch (Exception e) {
            throw new BizException(ErrorCode.INVALID_PARAM, "payload 格式错误，请检查上传参数");
        }
    }

    @Data
    public static class UploadPayload {
        private String plateNumber;
        private String customNumber;

        private Long brandId;
        private String brandName;

        private Long modelId;
        private String modelName;

        private Long companyId;
        private String companyName;

        private Long regionId;
        private String regionProvince;
        private String regionCity;

        private LocalDate factoryDate;
        private LocalDate launchDate;
        private Boolean airConditioned;
        private String source;
        private String remark;

        private VehicleConfigPayload config;
        private String uploadUser;

        void validate() {
            if (!StringUtils.hasText(plateNumber)) {
                throw new BizException(ErrorCode.INVALID_PARAM, "Plate number is required");
            }
            if (modelId == null && !StringUtils.hasText(modelName)) {
                throw new BizException(ErrorCode.INVALID_PARAM, "Model name or ID is required");
            }
            if (companyId == null && !StringUtils.hasText(companyName)) {
                throw new BizException(ErrorCode.INVALID_PARAM, "Company name or ID is required");
            }
            if (regionId == null && !StringUtils.hasText(regionCity)) {
                throw new BizException(ErrorCode.INVALID_PARAM, "Region or city is required");
            }
        }

        Vehicle toVehicle() {
            Vehicle vehicle = new Vehicle();
            vehicle.setPlateNumber(plateNumber);
            vehicle.setCustomNumber(customNumber);
            vehicle.setFactoryDate(factoryDate);
            vehicle.setLaunchDate(launchDate);
            vehicle.setAirConditioned(Boolean.TRUE.equals(airConditioned));
            vehicle.setSource(StringUtils.hasText(source) ? source : "用户上传");
            vehicle.setRemark(remark);

            if (modelId != null) {
                Model model = new Model();
                model.setId(modelId);
                vehicle.setModel(model);
            }
            if (companyId != null) {
                Company company = new Company();
                company.setId(companyId);
                vehicle.setCompany(company);
            }
            if (regionId != null) {
                Region region = new Region();
                region.setId(regionId);
                vehicle.setRegion(region);
            }
            return vehicle;
        }

        VehicleConfig toVehicleConfig() {
            if (config == null) {
                return null;
            }
            VehicleConfig cfg = new VehicleConfig();
            if (config.getBrandId() != null) {
                Brand brand = new Brand();
                brand.setId(config.getBrandId());
                cfg.setBrand(brand);
            }
            if (config.getModelId() != null) {
                Model model = new Model();
                model.setId(config.getModelId());
                cfg.setModel(model);
            }
            cfg.setMotor(config.getMotor());
            cfg.setEngine(config.getEngine());
            cfg.setFuelType(config.getFuelType());
            cfg.setStepType(config.getStepType());
            cfg.setTransmissionSystem(config.getTransmissionSystem());
            cfg.setSuspension(config.getSuspension());
            cfg.setAxle(config.getAxle());
            cfg.setOtherConfigs(config.getOtherConfigs());
            return cfg;
        }
    }

    @Data
    public static class VehicleConfigPayload {
        private Long brandId;
        private Long modelId;
        private String motor;
        private String engine;
        private String fuelType;
        private String stepType;
        private String transmissionSystem;
        private String suspension;
        private String axle;
        private String otherConfigs;
    }
}
