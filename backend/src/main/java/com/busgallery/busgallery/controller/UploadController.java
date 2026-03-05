package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.VehicleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final VehicleService vehicleService;
    private final ImageService imageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VehicleController.VehicleDetailResponse uploadVehicle(
            @RequestPart("file") MultipartFile file,
            @RequestPart("payload") UploadPayload payload
    ) {
        payload.validate();

        Image image = imageService.uploadAndSave(file, payload.getUploadUser());
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
                throw new IllegalArgumentException("车牌号不能为空");
            }
            if (modelId == null && !StringUtils.hasText(modelName)) {
                throw new IllegalArgumentException("车型名称或ID至少提供一个");
            }
            if (companyId == null && !StringUtils.hasText(companyName)) {
                throw new IllegalArgumentException("公司名称或ID至少提供一个");
            }
            if (regionId == null && !StringUtils.hasText(regionCity)) {
                throw new IllegalArgumentException("必须提供城市或地区ID");
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
        private String suspension;
        private String axle;
        private String otherConfigs;
    }
}