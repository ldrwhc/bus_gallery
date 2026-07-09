package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.VehicleRoute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VehicleRouteMapper {

    List<VehicleRoute> selectByVehicleId(@Param("vehicleId") Long vehicleId);

    List<VehicleRoute> selectByRouteId(@Param("routeId") Long routeId);

    VehicleRoute selectByVehicleAndRoute(@Param("vehicleId") Long vehicleId,
                                          @Param("routeId") Long routeId);

    int insert(VehicleRoute vr);

    int delete(@Param("id") Long id);

    int deleteByVehicleId(@Param("vehicleId") Long vehicleId);

    int deleteByRouteId(@Param("routeId") Long routeId);

    int deleteByVehicleAndRoute(@Param("vehicleId") Long vehicleId,
                                 @Param("routeId") Long routeId);
}
