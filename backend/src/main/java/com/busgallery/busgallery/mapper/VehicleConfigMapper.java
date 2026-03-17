package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.VehicleConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * VehicleConfigMapper接口用于封装VehicleConfigMapper相关的领域职责（所在包：com.busgallery.busgallery.mapper）。
 */
@Mapper
public interface VehicleConfigMapper {

    /**
     * selectByVehicleId方法用于处理selectByVehicleId相关的业务逻辑。
     * @param vehicleId vehicleId参数，详见调用方上下文。
     * @return 返回VehicleConfig类型结果。
     */
    VehicleConfig selectByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * insert方法用于处理insert相关的业务逻辑。
     * @param vehicleConfig vehicleConfig参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int insert(VehicleConfig vehicleConfig);

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param vehicleConfig vehicleConfig参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int update(VehicleConfig vehicleConfig);

    /**
     * deleteByVehicleId方法用于处理deleteByVehicleId相关的业务逻辑。
     * @param vehicleId vehicleId参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int deleteByVehicleId(@Param("vehicleId") Long vehicleId);
}