package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.VehicleFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VehicleFavoriteMapper {
    VehicleFavorite select(@Param("vehicleId") Long vehicleId, @Param("userId") Long userId);

    List<VehicleFavorite> selectByVehicle(@Param("vehicleId") Long vehicleId, @Param("limit") int limit);

    List<VehicleFavorite> selectByUser(@Param("userId") Long userId);

    long countByVehicle(@Param("vehicleId") Long vehicleId);

    int insert(VehicleFavorite favorite);

    int delete(@Param("vehicleId") Long vehicleId, @Param("userId") Long userId);

    int deleteByVehicleId(@Param("vehicleId") Long vehicleId);
}
