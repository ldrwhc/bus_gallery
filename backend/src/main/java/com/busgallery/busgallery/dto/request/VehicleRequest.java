package com.busgallery.busgallery.dto.request;

import com.busgallery.busgallery.entity.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class VehicleRequest {

    private String plateNumber;
    private String customNumber;
    private Long modelId;
    private Long companyId;
    private Long regionId;
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
}