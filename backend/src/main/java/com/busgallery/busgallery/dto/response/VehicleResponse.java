package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.entity.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * VehicleResponse类用于封装VehicleResponse相关的领域职责（所在包：com.busgallery.busgallery.dto.response）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {

    private Long id;
    private String plateNumber;
    private String customNumber;
    private Long modelId;
    private String modelName;
    private Long companyId;
    private String companyName;
    private Long regionId;
    private String regionName;
    private LocalDate factoryDate;
    private LocalDate launchDate;
    private Boolean airConditioned;
    private String source;
    private String remark;
    private VehicleConfigResponse config;
    private List<ImageResponse> images;

    /**
     * from方法用于处理from相关的业务逻辑。
     * @param vehicle vehicle参数，详见调用方上下文。
     * @param config config参数，详见调用方上下文。
     * @param imageList imageList参数，详见调用方上下文。
     * @return 返回VehicleResponse类型结果。
     */
    public static VehicleResponse from(Vehicle vehicle, VehicleConfig config, List<Image> imageList) {
        if (vehicle == null) {
            return null;
        }
        VehicleResponse resp = new VehicleResponse();
        resp.setId(vehicle.getId());
        resp.setPlateNumber(vehicle.getPlateNumber());
        resp.setCustomNumber(vehicle.getCustomNumber());
        if (vehicle.getModel() != null) {
            resp.setModelId(vehicle.getModel().getId());
            resp.setModelName(vehicle.getModel().getName());
        }
        if (vehicle.getCompany() != null) {
            resp.setCompanyId(vehicle.getCompany().getId());
            resp.setCompanyName(vehicle.getCompany().getName());
        }
        if (vehicle.getRegion() != null) {
            resp.setRegionId(vehicle.getRegion().getId());
            resp.setRegionName(vehicle.getRegion().getName());
        }
        resp.setFactoryDate(vehicle.getFactoryDate());
        resp.setLaunchDate(vehicle.getLaunchDate());
        resp.setAirConditioned(vehicle.getAirConditioned());
        resp.setSource(vehicle.getSource());
        resp.setRemark(vehicle.getRemark());
        resp.setConfig(VehicleConfigResponse.fromEntity(config));
        if (imageList != null) {
            resp.setImages(imageList.stream().map(ImageResponse::fromEntity).collect(Collectors.toList()));
        }
        return resp;
    }
}