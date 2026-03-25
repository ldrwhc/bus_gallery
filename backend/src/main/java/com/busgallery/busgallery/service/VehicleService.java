package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.entity.VehicleConfig;

import java.util.List;

public interface VehicleService {

    Vehicle findById(Long id);

    Vehicle findByPlateNumber(String plateNumber);

    List<Vehicle> listByPlateNumber(String plateNumber);

    List<Vehicle> listByRegion(Long regionId);

    List<Vehicle> listByCompany(Long companyId);

    List<Vehicle> listByModel(Long modelId);

    List<Vehicle> queryPage(int size, Long regionId, Long companyId, Long brandId, Long modelId, String keyword, java.time.LocalDate lastLaunch, Long lastId);

    List<Vehicle> listHotByViewCount(int limit);

    long count(Long regionId, Long companyId, Long brandId, Long modelId, String keyword);

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

    void recordView(Long vehicleId, String clientFingerprint);
}
