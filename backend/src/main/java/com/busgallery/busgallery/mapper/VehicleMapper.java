package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VehicleMapper {

    Vehicle selectById(@Param("id") Long id);

    Vehicle selectByPlateNumber(@Param("plateNumber") String plateNumber);

    List<Vehicle> selectByRegionId(@Param("regionId") Long regionId);

    List<Vehicle> selectByCompanyId(@Param("companyId") Long companyId);

    List<Vehicle> selectByModelId(@Param("modelId") Long modelId);

    List<Vehicle> selectPage(@Param("offset") int offset,
                             @Param("limit") int limit,
                             @Param("regionId") Long regionId,
                             @Param("companyId") Long companyId,
                             @Param("brandId") Long brandId,
                             @Param("modelId") Long modelId);

    long count(@Param("regionId") Long regionId,
               @Param("companyId") Long companyId,
               @Param("brandId") Long brandId,
               @Param("modelId") Long modelId);

    int insert(Vehicle vehicle);

    int update(Vehicle vehicle);

    int delete(@Param("id") Long id);
}