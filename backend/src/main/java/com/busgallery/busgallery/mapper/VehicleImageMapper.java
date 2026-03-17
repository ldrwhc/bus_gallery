package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.VehicleImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * VehicleImageMapper接口用于封装VehicleImageMapper相关的领域职责（所在包：com.busgallery.busgallery.mapper）。
 */
@Mapper
public interface VehicleImageMapper {

    /**
     * selectByVehicleId方法用于处理selectByVehicleId相关的业务逻辑。
     * @param vehicleId vehicleId参数，详见调用方上下文。
     * @return 返回List<VehicleImage>类型结果。
     */
    List<VehicleImage> selectByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * insert方法用于处理insert相关的业务逻辑。
     * @param vehicleImage vehicleImage参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int insert(VehicleImage vehicleImage);

    /**
     * 批量插入车辆-图片关联，参数名固定为 vehicleImages，便于 XML 中引用
     */
    /**
     * insertBatch方法用于处理insertBatch相关的业务逻辑。
     * @param vehicleImages vehicleImages参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int insertBatch(@Param("vehicleImages") List<VehicleImage> vehicleImages);

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param vehicleId vehicleId参数，详见调用方上下文。
     * @param imageId imageId参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int delete(@Param("vehicleId") Long vehicleId, @Param("imageId") Long imageId);

    /**
     * deleteByVehicleId方法用于处理deleteByVehicleId相关的业务逻辑。
     * @param vehicleId vehicleId参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int deleteByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * deleteByImageId方法用于处理deleteByImageId相关的业务逻辑。
     * @param imageId imageId参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int deleteByImageId(@Param("imageId") Long imageId);
}