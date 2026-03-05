package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.entity.VehicleConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleConfigResponse {

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

    public static VehicleConfigResponse fromEntity(VehicleConfig config) {
        if (config == null) {
            return null;
        }
        Long brandId = config.getBrand() != null ? config.getBrand().getId() : null;
        String brandName = config.getBrand() != null ? config.getBrand().getName() : null;
        Long modelId = config.getModel() != null ? config.getModel().getId() : null;
        String modelName = config.getModel() != null ? config.getModel().getName() : null;

        return new VehicleConfigResponse(
                brandId, brandName, modelId, modelName,
                config.getMotor(), config.getEngine(), config.getFuelType(),
                config.getStepType(), config.getSuspension(), config.getAxle(),
                config.getOtherConfigs()
        );
    }
}