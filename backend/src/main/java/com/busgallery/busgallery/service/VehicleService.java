package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.entity.VehicleConfig;

import java.util.List;

/**
 * VehicleService接口用于封装VehicleService相关的领域职责（所在包：com.busgallery.busgallery.service）。
 */
public interface VehicleService {

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Vehicle类型结果。
     */
    Vehicle findById(Long id);

    /**
     * findByPlateNumber方法用于处理findByPlateNumber相关的业务逻辑。
     * @param plateNumber plateNumber参数，详见调用方上下文。
     * @return 返回Vehicle类型结果。
     */
    Vehicle findByPlateNumber(String plateNumber);

    /**
     * listByRegion方法用于处理listByRegion相关的业务逻辑。
     * @param regionId regionId参数，详见调用方上下文。
     * @return 返回List<Vehicle>类型结果。
     */
    List<Vehicle> listByRegion(Long regionId);

    /**
     * listByCompany方法用于处理listByCompany相关的业务逻辑。
     * @param companyId companyId参数，详见调用方上下文。
     * @return 返回List<Vehicle>类型结果。
     */
    List<Vehicle> listByCompany(Long companyId);

    /**
     * listByModel方法用于处理listByModel相关的业务逻辑。
     * @param modelId modelId参数，详见调用方上下文。
     * @return 返回List<Vehicle>类型结果。
     */
    List<Vehicle> listByModel(Long modelId);

    /**
     * queryPage方法用于处理queryPage相关的业务逻辑。
     * @param page page参数，详见调用方上下文。
     * @param size size参数，详见调用方上下文。
     * @param regionId regionId参数，详见调用方上下文。
     * @param companyId companyId参数，详见调用方上下文。
     * @param brandId brandId参数，详见调用方上下文。
     * @param modelId modelId参数，详见调用方上下文。
     * @param keyword keyword参数，详见调用方上下文。
     * @return 返回List<Vehicle>类型结果。
     */
    List<Vehicle> queryPage(int page, int size, Long regionId, Long companyId, Long brandId, Long modelId, String keyword);

    /**
     * count方法用于处理count相关的业务逻辑。
     * @param regionId regionId参数，详见调用方上下文。
     * @param companyId companyId参数，详见调用方上下文。
     * @param brandId brandId参数，详见调用方上下文。
     * @param modelId modelId参数，详见调用方上下文。
     * @param keyword keyword参数，详见调用方上下文。
     * @return 返回long类型结果。
     */
    long count(Long regionId, Long companyId, Long brandId, Long modelId, String keyword);

    /**
     * findConfigByVehicleId方法用于处理findConfigByVehicleId相关的业务逻辑。
     * @param vehicleId vehicleId参数，详见调用方上下文。
     * @return 返回VehicleConfig类型结果。
     */
    VehicleConfig findConfigByVehicleId(Long vehicleId);

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param vehicle vehicle参数，详见调用方上下文。
     * @param config config参数，详见调用方上下文。
     * @param imageIds imageIds参数，详见调用方上下文。
     * @param brandId brandId参数，详见调用方上下文。
     * @param brandName brandName参数，详见调用方上下文。
     * @param modelName modelName参数，详见调用方上下文。
     * @param companyName companyName参数，详见调用方上下文。
     * @param regionProvince regionProvince参数，详见调用方上下文。
     * @param regionCity regionCity参数，详见调用方上下文。
     * @return 返回Vehicle类型结果。
     */
    Vehicle create(Vehicle vehicle,
                   VehicleConfig config,
                   List<Long> imageIds,
                   Long brandId,
                   String brandName,
                   String modelName,
                   String companyName,
                   String regionProvince,
                   String regionCity);

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param vehicle vehicle参数，详见调用方上下文。
     * @param config config参数，详见调用方上下文。
     * @param imageIds imageIds参数，详见调用方上下文。
     * @param brandId brandId参数，详见调用方上下文。
     * @param brandName brandName参数，详见调用方上下文。
     * @param modelName modelName参数，详见调用方上下文。
     * @param companyName companyName参数，详见调用方上下文。
     * @param regionProvince regionProvince参数，详见调用方上下文。
     * @param regionCity regionCity参数，详见调用方上下文。
     * @return 返回Vehicle类型结果。
     */
    Vehicle update(Vehicle vehicle,
                   VehicleConfig config,
                   List<Long> imageIds,
                   Long brandId,
                   String brandName,
                   String modelName,
                   String companyName,
                   String regionProvince,
                   String regionCity);

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    void delete(Long id);
}
