package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.entity.VehicleConfig;
import com.busgallery.busgallery.util.FuelTypeNormalizer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VehicleConfigResponse类用于封装VehicleConfigResponse相关的领域职责（所在包：com.busgallery.busgallery.dto.response）。
 */
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

    /**
     * fromEntity方法用于处理fromEntity相关的业务逻辑。
     * @param config config参数，详见调用方上下文。
     * @return 返回VehicleConfigResponse类型结果。
     */
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
                config.getMotor(), config.getEngine(), FuelTypeNormalizer.normalize(config.getFuelType()),
                config.getStepType(), config.getSuspension(), config.getAxle(),
                config.getOtherConfigs()
        );
    }
}
