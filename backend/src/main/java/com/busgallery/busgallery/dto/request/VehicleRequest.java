package com.busgallery.busgallery.dto.request;

import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.util.FuelTypeNormalizer;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * VehicleRequest类用于封装VehicleRequest相关的领域职责（所在包：com.busgallery.busgallery.dto.request）。
 */
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

    /**
     * toVehicle方法用于处理toVehicle相关的业务逻辑。
     * @return 返回Vehicle类型结果。
     */
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

    /**
     * toVehicleConfig方法用于处理toVehicleConfig相关的业务逻辑。
     * @return 返回VehicleConfig类型结果。
     */
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
        cfg.setSuspension(config.getSuspension());
        cfg.setAxle(config.getAxle());
        cfg.setOtherConfigs(config.getOtherConfigs());
        return cfg;
    }

    /**
     * VehicleConfigRequest类用于封装VehicleConfigRequest相关的领域职责（所在包：com.busgallery.busgallery.dto.request）。
     */
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
