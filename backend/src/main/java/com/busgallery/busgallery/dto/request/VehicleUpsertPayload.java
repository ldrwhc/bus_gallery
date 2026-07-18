package com.busgallery.busgallery.dto.request;

import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.util.FuelTypeNormalizer;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Data
public class VehicleUpsertPayload {
    private Long vehicleId;
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
    private List<Long> imageIds;
    private List<RouteAssignmentPayload> routes;

    @Data
    public static class RouteAssignmentPayload {
        private Long routeId;
        private String routeNumber;
        private String routeName;
        private String startStop;
        private String endStop;
        private String downStartStop;
        private String downEndStop;
        private String routeType;
        private String subType;
        private Long parentRouteId;
        private Boolean isLoop;
        private Boolean isCurrent;
        private Boolean isActive;
        private java.math.BigDecimal lineLengthKm;
        private String ticketType;
        private String ticketPrice;
        private String operatingHours;
        private String firstOperated;
        private String lastOperated;
        private String remark;
    }

    public void validate() {
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

    public Vehicle toVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
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
