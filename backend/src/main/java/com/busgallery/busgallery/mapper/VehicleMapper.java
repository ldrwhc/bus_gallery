package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface VehicleMapper {

    Vehicle selectById(@Param("id") Long id);

    Vehicle selectDetailById(@Param("id") Long id);

    Vehicle selectByPlateNumber(@Param("plateNumber") String plateNumber);

    List<Vehicle> selectAllByPlateNumber(@Param("plateNumber") String plateNumber);

    List<Vehicle> selectDetailByPlateNumber(@Param("plateNumber") String plateNumber);

    List<Vehicle> selectByRegionId(@Param("regionId") Long regionId);

    List<Vehicle> selectByCompanyId(@Param("companyId") Long companyId);

    List<Vehicle> selectByModelId(@Param("modelId") Long modelId);

    List<Vehicle> selectPageByCursor(@Param("limit") int limit,
                                     @Param("regionId") Long regionId,
                                     @Param("companyId") Long companyId,
                                     @Param("brandId") Long brandId,
                                     @Param("modelId") Long modelId,
                                     @Param("keyword") String keyword,
                                     @Param("lastLaunch") LocalDate lastLaunch,
                                     @Param("lastId") Long lastId);

    long count(@Param("regionId") Long regionId,
               @Param("companyId") Long companyId,
               @Param("brandId") Long brandId,
               @Param("modelId") Long modelId,
               @Param("keyword") String keyword);

    int insert(Vehicle vehicle);

    int update(Vehicle vehicle);

    int delete(@Param("id") Long id);
}
