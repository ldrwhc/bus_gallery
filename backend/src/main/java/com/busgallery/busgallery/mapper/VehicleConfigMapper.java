package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.VehicleConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VehicleConfigMapper {

    VehicleConfig selectByVehicleId(@Param("vehicleId") Long vehicleId);

    int insert(VehicleConfig vehicleConfig);

    int update(VehicleConfig vehicleConfig);

    int deleteByVehicleId(@Param("vehicleId") Long vehicleId);
}