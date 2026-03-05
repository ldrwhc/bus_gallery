package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.entity.VehicleConfig;

import java.util.List;

public interface VehicleService {

    Vehicle findById(Long id);

    Vehicle findByPlateNumber(String plateNumber);

    List<Vehicle> listByRegion(Long regionId);

    List<Vehicle> listByCompany(Long companyId);

    List<Vehicle> listByModel(Long modelId);

    List<Vehicle> queryPage(int page, int size, Long regionId, Long companyId, Long brandId, Long modelId);

    long count(Long regionId, Long companyId, Long brandId, Long modelId);

    VehicleConfig findConfigByVehicleId(Long vehicleId);

    Vehicle create(Vehicle vehicle,
                   VehicleConfig config,
                   List<Long> imageIds,
                   Long brandId,
                   String brandName,
                   String modelName,
                   String companyName,
                   String regionProvince,
                   String regionCity);

    Vehicle update(Vehicle vehicle,
                   VehicleConfig config,
                   List<Long> imageIds,
                   Long brandId,
                   String brandName,
                   String modelName,
                   String companyName,
                   String regionProvince,
                   String regionCity);

    void delete(Long id);
}