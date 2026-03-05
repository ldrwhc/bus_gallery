package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.VehicleService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final ImageService imageService;

    @GetMapping("/{id}")
    public VehicleDetailResponse detail(@PathVariable Long id) {
        return buildVehicleDetail(id);
    }

    @GetMapping
    public VehiclePageResponse page(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) Long regionId,
                                    @RequestParam(required = false) Long companyId,
                                    @RequestParam(required = false) Long brandId,
                                    @RequestParam(required = false) Long modelId) {
        List<Vehicle> vehicles = vehicleService.queryPage(page, size, regionId, companyId, brandId, modelId);
        long total = vehicleService.count(regionId, companyId, brandId, modelId);
        List<VehicleSummary> summaries = vehicles.stream()
                .map(vehicle -> new VehicleSummary(mapVehicle(vehicle), mapImages(imageService.listByVehicle(vehicle.getId()))))
                .collect(Collectors.toList());
        return new VehiclePageResponse(summaries, total, page, size);
    }

    @PostMapping
    public VehicleDetailResponse create(@Valid @RequestBody VehicleRequest request) {
        Vehicle vehicle = request.toVehicle();
        VehicleConfig config = request.toVehicleConfig();
        Vehicle saved = vehicleService.create(
                vehicle,
                config,
                request.getImageIds(),
                request.getBrandId(),
                request.getBrandName(),
                request.getModelName(),
                request.getCompanyName(),
                request.getRegionProvince(),
                request.getRegionCity()
        );
        return buildVehicleDetail(saved.getId());
    }

    @PutMapping("/{id}")
    public VehicleDetailResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        Vehicle vehicle = request.toVehicle();
        vehicle.setId(id);
        VehicleConfig config = request.toVehicleConfig();
        Vehicle updated = vehicleService.update(
                vehicle,
                config,
                request.getImageIds(),
                request.getBrandId(),
                request.getBrandName(),
                request.getModelName(),
                request.getCompanyName(),
                request.getRegionProvince(),
                request.getRegionCity()
        );
        return buildVehicleDetail(updated.getId());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        vehicleService.delete(id);
    }

    private VehicleDetailResponse buildVehicleDetail(Long vehicleId) {
        Vehicle vehicle = vehicleService.findById(vehicleId);
        VehicleConfig config = vehicleService.findConfigByVehicleId(vehicleId);
        List<Image> images = imageService.listByVehicle(vehicleId);
        return assembleDetail(vehicle, config, images);
    }

    /** 提供给其它控制器复用的装配方法（如上传接口） */
    public static VehicleDetailResponse assembleDetail(Vehicle vehicle,
                                                       VehicleConfig config,
                                                       List<Image> images) {
        return new VehicleDetailResponse(
                mapVehicle(vehicle),
                mapVehicleConfig(config),
                mapImages(images)
        );
    }

    private static VehicleDTO mapVehicle(Vehicle source) {
        if (source == null) {
            return null;
        }
        VehicleDTO dto = new VehicleDTO();
        dto.setId(source.getId());
        dto.setPlateNumber(source.getPlateNumber());
        dto.setCustomNumber(source.getCustomNumber());
        dto.setFactoryDate(source.getFactoryDate());
        dto.setLaunchDate(source.getLaunchDate());
        dto.setAirConditioned(source.getAirConditioned());
        dto.setSource(source.getSource());
        dto.setRemark(source.getRemark());
        dto.setCreatedAt(source.getCreatedAt());
        dto.setUpdatedAt(source.getUpdatedAt());
        dto.setModel(mapModel(source.getModel()));
        dto.setCompany(mapCompany(source.getCompany()));
        dto.setRegion(mapRegion(source.getRegion()));
        return dto;
    }

    private static ModelDTO mapModel(Model source) {
        if (source == null) {
            return null;
        }
        ModelDTO dto = new ModelDTO();
        dto.setId(source.getId());
        dto.setName(source.getName());
        dto.setBrandId(source.getBrand() != null ? source.getBrand().getId() : null);
        dto.setBrandName(source.getBrand() != null ? source.getBrand().getName() : null);
        return dto;
    }

    private static CompanyDTO mapCompany(Company source) {
        if (source == null) {
            return null;
        }
        CompanyDTO dto = new CompanyDTO();
        dto.setId(source.getId());
        dto.setName(source.getName());
        dto.setRegionId(source.getRegion() != null ? source.getRegion().getId() : null);
        dto.setRegionName(source.getRegion() != null ? source.getRegion().getName() : null);
        return dto;
    }

    private static RegionDTO mapRegion(Region source) {
        if (source == null) {
            return null;
        }
        RegionDTO dto = new RegionDTO();
        dto.setId(source.getId());
        dto.setName(source.getName());
        dto.setLevel(source.getLevel());
        dto.setParentId(source.getParentId());
        return dto;
    }

    private static VehicleConfigDTO mapVehicleConfig(VehicleConfig source) {
        if (source == null) {
            return null;
        }
        VehicleConfigDTO dto = new VehicleConfigDTO();
        dto.setId(source.getId());
        dto.setVehicleId(source.getVehicle() != null ? source.getVehicle().getId() : null);
        dto.setBrandId(source.getBrand() != null ? source.getBrand().getId() : null);
        dto.setBrandName(source.getBrand() != null ? source.getBrand().getName() : null);
        dto.setModelId(source.getModel() != null ? source.getModel().getId() : null);
        dto.setModelName(source.getModel() != null ? source.getModel().getName() : null);
        dto.setMotor(source.getMotor());
        dto.setEngine(source.getEngine());
        dto.setFuelType(source.getFuelType());
        dto.setStepType(source.getStepType());
        dto.setSuspension(source.getSuspension());
        dto.setAxle(source.getAxle());
        dto.setOtherConfigs(source.getOtherConfigs());
        return dto;
    }

    private static List<ImageDTO> mapImages(List<Image> images) {
        if (Objects.isNull(images)) {
            return Collections.emptyList();
        }
        return images.stream()
                .map(VehicleController::mapImage)
                .collect(Collectors.toList());
    }

    private static ImageDTO mapImage(Image source) {
        if (source == null) {
            return null;
        }
        ImageDTO dto = new ImageDTO();
        dto.setId(source.getId());
        dto.setObjectName(source.getObjectName());
        dto.setUrl(source.getUrl());
        dto.setThumbnailUrl(source.getThumbnailUrl());
        dto.setSizeBytes(source.getSizeBytes());
        dto.setWidth(source.getWidth());
        dto.setHeight(source.getHeight());
        dto.setMimeType(source.getMimeType());
        dto.setHash(source.getHash());
        dto.setUploadUser(source.getUploadUser());
        dto.setCreateTime(source.getCreateTime());
        return dto;
    }

    /* --------- 以下 DTO / 请求类定义保持不变 --------- */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VehiclePageResponse {
        private List<VehicleSummary> records = Collections.emptyList();
        private long total;
        private int page;
        private int size;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VehicleSummary {
        private VehicleDTO vehicle;
        private List<ImageDTO> images;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VehicleDetailResponse {
        private VehicleDTO vehicle;
        private VehicleConfigDTO vehicleConfig;
        private List<ImageDTO> images;
    }

    @Data
    public static class VehicleRequest {
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
        private VehicleConfigRequest config;
        private List<Long> imageIds;

        public Vehicle toVehicle() {
            Vehicle vehicle = new Vehicle();
            vehicle.setPlateNumber(plateNumber);
            vehicle.setCustomNumber(customNumber);
            vehicle.setFactoryDate(factoryDate);
            vehicle.setLaunchDate(launchDate);
            vehicle.setAirConditioned(Boolean.TRUE.equals(airConditioned));
            vehicle.setSource(source);
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

        public VehicleConfig toVehicleConfig() {
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
    public static class VehicleConfigRequest {
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

    @Data
    public static class VehicleDTO {
        private Long id;
        private String plateNumber;
        private String customNumber;
        private LocalDate factoryDate;
        private LocalDate launchDate;
        private Boolean airConditioned;
        private String source;
        private String remark;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
        private ModelDTO model;
        private CompanyDTO company;
        private RegionDTO region;
    }

    @Data
    public static class ModelDTO {
        private Long id;
        private String name;
        private Long brandId;
        private String brandName;
    }

    @Data
    public static class CompanyDTO {
        private Long id;
        private String name;
        private Long regionId;
        private String regionName;
    }

    @Data
    public static class RegionDTO {
        private Long id;
        private String name;
        private Long parentId;
        private Integer level;
    }

    @Data
    public static class VehicleConfigDTO {
        private Long id;
        private Long vehicleId;
        private Long brandId;
        private String brandName;
        private Long modelId;
        private String modelName;
        private String motor;
        private String engine;
        private String fuelType;
        private String stepType;
        private String suspension;
        private String axle;
        private String otherConfigs;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;
    }

    @Data
    public static class ImageDTO {
        private Long id;
        private String objectName;
        private String url;
        private String thumbnailUrl;
        private Long sizeBytes;
        private Integer width;
        private Integer height;
        private String mimeType;
        private String hash;
        private String uploadUser;
        private java.time.LocalDateTime createTime;
    }
}