package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.VehicleImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VehicleImageMapper {

    List<VehicleImage> selectByVehicleId(@Param("vehicleId") Long vehicleId);

    int insert(VehicleImage vehicleImage);

    /**
     * 批量插入车辆-图片关联，参数名固定为 vehicleImages，便于 XML 中引用
     */
    int insertBatch(@Param("vehicleImages") List<VehicleImage> vehicleImages);

    int delete(@Param("vehicleId") Long vehicleId, @Param("imageId") Long imageId);

    int deleteByVehicleId(@Param("vehicleId") Long vehicleId);

    int deleteByImageId(@Param("imageId") Long imageId);
}