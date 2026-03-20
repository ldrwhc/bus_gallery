package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.RoleGuard;
import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.RegionService;
import com.busgallery.busgallery.service.VehicleService;
import com.busgallery.busgallery.util.ExifUtils;
import com.busgallery.busgallery.util.FuelTypeNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final ImageService imageService;
    private final RegionService regionService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${busgallery.cache.vehicles.page-ttl-seconds:60}")
    private long vehiclePageCacheTtlSeconds;

    @GetMapping("/{id}")
    public VehicleDetailResponse detail(@PathVariable Long id) {
        return buildVehicleDetail(id);
    }

    @GetMapping("/plate/{plateNumber}")
    public VehicleGroupResponse detailByPlate(@PathVariable String plateNumber) {
        String normalized = plateNumber == null ? null : plateNumber.replaceAll("\\s+", "");
        List<Vehicle> vehicles = vehicleService.listByPlateNumber(normalized);
        List<VehicleDetailResponse> variants = vehicles.stream()
                .map(v -> buildVehicleDetail(v.getId()))
                .collect(Collectors.toList());
        return new VehicleGroupResponse(plateNumber, variants);
    }

    @GetMapping
    public VehiclePageResponse page(@RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) Long regionId,
                                    @RequestParam(required = false) Long companyId,
                                    @RequestParam(required = false) Long brandId,
                                    @RequestParam(required = false) Long modelId,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) LocalDate lastLaunch,
                                    @RequestParam(required = false) Long lastId) {
        String cacheKey = buildPageCacheKey(size, regionId, companyId, brandId, modelId, keyword, lastLaunch, lastId);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                return objectMapper.readValue(cached, VehiclePageResponse.class);
            }
        } catch (Exception ignore) {
        }
        List<Vehicle> vehicles = vehicleService.queryPage(size, regionId, companyId, brandId, modelId, keyword, lastLaunch, lastId);
        long total = vehicleService.count(regionId, companyId, brandId, modelId, keyword);
        List<VehicleSummary> summaries = vehicles.stream()
                .map(vehicle -> new VehicleSummary(mapVehicle(vehicle), mapImages(imageService.listByVehicle(vehicle.getId()))))
                .collect(Collectors.toList());
        java.time.LocalDate nextLaunch = vehicles.isEmpty() ? null : vehicles.get(vehicles.size() - 1).getLaunchDate();
        Long nextCursorId = vehicles.isEmpty() ? null : vehicles.get(vehicles.size() - 1).getId();
        VehiclePageResponse response = new VehiclePageResponse(summaries, total, 1, size, nextLaunch, nextCursorId);
        try {
            String payload = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(cacheKey, payload, Duration.ofSeconds(vehiclePageCacheTtlSeconds));
        } catch (Exception ignore) {
        }
        return response;
    }

    private String buildPageCacheKey(int size,
                                     Long regionId,
                                     Long companyId,
                                     Long brandId,
                                     Long modelId,
                                     String keyword,
                                     java.time.LocalDate lastLaunch,
                                     Long lastId) {
        String version = "1";
        try {
            String v = stringRedisTemplate.opsForValue().get("bg:vehicle:page:version");
            if (v != null && !v.isBlank()) {
                version = v;
            }
        } catch (Exception ignore) {
        }
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        return String.format(
                "bg:vehicle:page:v%s:s%d:r%s:c%s:b%s:m%s:k%s:l%s:i%s",
                version,
                size,
                regionId == null ? "0" : regionId,
                companyId == null ? "0" : companyId,
                brandId == null ? "0" : brandId,
                modelId == null ? "0" : modelId,
                kw.isEmpty() ? "-" : kw,
                lastLaunch == null ? "-" : lastLaunch,
                lastId == null ? "-" : lastId
        );
    }

    @PostMapping
    @RequireLogin
    public VehicleDetailResponse create(@Valid @RequestBody VehicleRequest request) {
        UserSession session = RoleGuard.requireReviewerOrStation();
        assertRegionWriteAllowed(session, request.getRegionId());
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
    @RequireLogin
    public VehicleDetailResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        UserSession session = RoleGuard.requireReviewerOrStation();
        Vehicle existing = vehicleService.findById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "车辆不存在");
        }
        assertRegionWriteAllowed(session, existing.getRegion() != null ? existing.getRegion().getId() : null);
        if (request.getRegionId() != null) {
            assertRegionWriteAllowed(session, request.getRegionId());
        }
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
    @RequireLogin
    public void delete(@PathVariable Long id) {
        UserSession session = RoleGuard.requireReviewerOrStation();
        Vehicle existing = vehicleService.findById(id);
        if (existing == null) {
            return;
        }
        assertRegionWriteAllowed(session, existing.getRegion() != null ? existing.getRegion().getId() : null);
        vehicleService.delete(id);
    }

    private void assertRegionWriteAllowed(UserSession session, Long targetRegionId) {
        if (session == null || session.getRole() == UserRole.STATION) {
            return;
        }
        if (session.getRole() != UserRole.REVIEWER) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "当前角色无权修改车辆信息");
        }
        if (session.getReviewRegionId() == null || targetRegionId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "无法确认审核地区权限");
        }
        Long reviewerProvinceId = regionService.resolveProvinceId(session.getReviewRegionId());
        Long targetProvinceId = regionService.resolveProvinceId(targetRegionId);
        if (reviewerProvinceId == null || targetProvinceId == null || !reviewerProvinceId.equals(targetProvinceId)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "目标车辆不在你的审核地区");
        }
    }

    private VehicleDetailResponse buildVehicleDetail(Long vehicleId) {
        Vehicle vehicle = vehicleService.findById(vehicleId);
        VehicleConfig config = vehicleService.findConfigByVehicleId(vehicleId);
        List<Image> images = imageService.listByVehicle(vehicleId);
        return assembleDetail(vehicle, config, images);
    }

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
        dto.setBrandChnName(source.getBrand() != null ? source.getBrand().getChnName() : null);
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
        dto.setBrandChnName(source.getBrand() != null ? source.getBrand().getChnName() : null);
        dto.setModelId(source.getModel() != null ? source.getModel().getId() : null);
        dto.setModelName(source.getModel() != null ? source.getModel().getName() : null);
        dto.setMotor(source.getMotor());
        dto.setEngine(source.getEngine());
        dto.setFuelType(FuelTypeNormalizer.normalize(source.getFuelType()));
        dto.setStepType(source.getStepType());
        dto.setTransmissionSystem(source.getTransmissionSystem());
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
        dto.setUploaderId(source.getUploaderId());
        dto.setUploaderUsername(source.getUploaderUsername());
        dto.setUploaderDisplayName(source.getUploaderDisplayName());
        dto.setCreateTime(source.getCreateTime());
        dto.setExif(ExifUtils.fromJson(source.getExifJson()));
        return dto;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VehiclePageResponse {
        private List<VehicleSummary> records = Collections.emptyList();
        private long total;
        private int page = 1;
        private int size;
        private java.time.LocalDate nextLaunch;
        private Long nextId;
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
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VehicleGroupResponse {
        private String plateNumber;
        private List<VehicleDetailResponse> variants;
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
            cfg.setFuelType(FuelTypeNormalizer.normalize(config.getFuelType()));
            cfg.setStepType(config.getStepType());
            cfg.setTransmissionSystem(config.getTransmissionSystem());
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
        private String transmissionSystem;
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
        private String brandChnName;
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
        private String brandChnName;
        private Long modelId;
        private String modelName;
        private String motor;
        private String engine;
        private String fuelType;
        private String stepType;
        private String transmissionSystem;
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
        private Long uploaderId;
        private String uploaderUsername;
        private String uploaderDisplayName;
        private java.time.LocalDateTime createTime;
        private Map<String, String> exif;
    }
}
