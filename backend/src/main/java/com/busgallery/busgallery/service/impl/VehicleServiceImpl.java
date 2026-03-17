package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.mapper.*;
import com.busgallery.busgallery.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleMapper vehicleMapper;
    private final VehicleConfigMapper vehicleConfigMapper;
    private final VehicleImageMapper vehicleImageMapper;
    private final BrandMapper brandMapper;
    private final ModelMapper modelMapper;
    private final CompanyMapper companyMapper;
    private final RegionMapper regionMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public Vehicle findById(Long id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        populateVehicleRelations(vehicle);
        return vehicle;
    }

    public Vehicle findByPlateNumber(String plateNumber) {
        Vehicle vehicle = vehicleMapper.selectByPlateNumber(plateNumber);
        populateVehicleRelations(vehicle);
        return vehicle;
    }

    public List<Vehicle> listByPlateNumber(String plateNumber) {
        List<Vehicle> vehicles = vehicleMapper.selectAllByPlateNumber(plateNumber);
        if (vehicles != null) {
            vehicles.forEach(this::populateVehicleRelations);
        }
        return vehicles;
    }

    public List<Vehicle> listByRegion(Long regionId) {
        List<Vehicle> list = vehicleMapper.selectByRegionId(regionId);
        list.forEach(this::populateVehicleRelations);
        return list;
    }

    public List<Vehicle> listByCompany(Long companyId) {
        List<Vehicle> list = vehicleMapper.selectByCompanyId(companyId);
        list.forEach(this::populateVehicleRelations);
        return list;
    }

    public List<Vehicle> listByModel(Long modelId) {
        List<Vehicle> list = vehicleMapper.selectByModelId(modelId);
        list.forEach(this::populateVehicleRelations);
        return list;
    }

    public List<Vehicle> queryPage(int size, Long regionId, Long companyId, Long brandId, Long modelId, String keyword, java.time.LocalDate lastLaunch, Long lastId) {
        int pageSize = Math.max(size, 1);
        List<Vehicle> list = vehicleMapper.selectPageByCursor(pageSize, regionId, companyId, brandId, modelId, keyword, lastLaunch, lastId);
        list.forEach(this::populateVehicleRelations);
        return list;
    }

    public long count(Long regionId, Long companyId, Long brandId, Long modelId, String keyword) {
        return vehicleMapper.count(regionId, companyId, brandId, modelId, keyword);
    }

    public VehicleConfig findConfigByVehicleId(Long vehicleId) {
        VehicleConfig config = vehicleConfigMapper.selectByVehicleId(vehicleId);
        populateVehicleConfigRelations(config);
        return config;
    }

    public Vehicle create(Vehicle vehicle,
                          VehicleConfig config,
                          List<Long> imageIds,
                          Long brandId,
                          String brandName,
                          String modelName,
                          String companyName,
                          String regionProvince,
                          String regionCity) {
        ensureRegionExists(vehicle, regionProvince, regionCity);
        ensureModelExists(vehicle, brandId, brandName, modelName);
        ensureCompanyExists(vehicle, companyName);
        vehicleMapper.insert(vehicle);
        upsertVehicleConfig(vehicle, config);
        saveVehicleImages(vehicle.getId(), imageIds);
        Vehicle saved = vehicleMapper.selectById(vehicle.getId());
        populateVehicleRelations(saved);
        bumpVehiclePageVersion();
        return saved;
    }

    public Vehicle update(Vehicle vehicle,
                          VehicleConfig config,
                          List<Long> imageIds,
                          Long brandId,
                          String brandName,
                          String modelName,
                          String companyName,
                          String regionProvince,
                          String regionCity) {
        ensureRegionExists(vehicle, regionProvince, regionCity);
        ensureModelExists(vehicle, brandId, brandName, modelName);
        ensureCompanyExists(vehicle, companyName);
        vehicleMapper.update(vehicle);
        upsertVehicleConfig(vehicle, config);
        saveVehicleImages(vehicle.getId(), imageIds);
        Vehicle saved = vehicleMapper.selectById(vehicle.getId());
        populateVehicleRelations(saved);
        bumpVehiclePageVersion();
        return saved;
    }

    public void delete(Long id) {
        vehicleImageMapper.deleteByVehicleId(id);
        vehicleConfigMapper.deleteByVehicleId(id);
        vehicleMapper.delete(id);
        bumpVehiclePageVersion();
    }

    private void bumpVehiclePageVersion() {
        try {
            stringRedisTemplate.opsForValue().increment("bg:vehicle:page:version");
        } catch (Exception ignore) {
        }
    }

    private void populateVehicleRelations(Vehicle vehicle) {
        if (vehicle == null) {
            return;
        }
        if (vehicle.getModel() != null && vehicle.getModel().getId() != null) {
            Model fullModel = modelMapper.selectById(vehicle.getModel().getId());
            if (fullModel != null && fullModel.getBrand() != null && fullModel.getBrand().getId() != null) {
                Brand brand = brandMapper.selectById(fullModel.getBrand().getId());
                fullModel.setBrand(brand);
            }
            vehicle.setModel(fullModel);
        }
        if (vehicle.getCompany() != null && vehicle.getCompany().getId() != null) {
            Company fullCompany = companyMapper.selectById(vehicle.getCompany().getId());
            populateCompanyRegion(fullCompany);
            vehicle.setCompany(fullCompany);
        }
        if (vehicle.getRegion() != null && vehicle.getRegion().getId() != null) {
            Region fullRegion = regionMapper.selectById(vehicle.getRegion().getId());
            vehicle.setRegion(fullRegion);
        }
    }

    private void populateVehicleConfigRelations(VehicleConfig config) {
        if (config == null) {
            return;
        }
        if (config.getBrand() != null && config.getBrand().getId() != null) {
            Brand brand = brandMapper.selectById(config.getBrand().getId());
            config.setBrand(brand);
        }
        if (config.getModel() != null && config.getModel().getId() != null) {
            Model model = modelMapper.selectById(config.getModel().getId());
            if (model != null && model.getBrand() != null && model.getBrand().getId() != null) {
                Brand brand = brandMapper.selectById(model.getBrand().getId());
                model.setBrand(brand);
            }
            config.setModel(model);
        }
    }

    private void populateCompanyRegion(Company company) {
        if (company == null) {
            return;
        }
        if (company.getRegion() != null && company.getRegion().getId() != null) {
            Region region = regionMapper.selectById(company.getRegion().getId());
            company.setRegion(region);
        }
    }

    private void ensureRegionExists(Vehicle vehicle, String provinceName, String cityName) {
        if (vehicle.getRegion() != null && vehicle.getRegion().getId() != null) {
            return;
        }
        if (!StringUtils.hasText(cityName)) {
            throw new IllegalArgumentException("城市名称不能为空");
        }
        Long regionId = findOrCreateRegion(provinceName, cityName.trim());
        Region region = new Region();
        region.setId(regionId);
        vehicle.setRegion(region);
    }

    private void ensureModelExists(Vehicle vehicle,
                                   Long brandId,
                                   String brandName,
                                   String modelName) {
        Long currentModelId = vehicle.getModel() != null ? vehicle.getModel().getId() : null;
        if (currentModelId != null) {
            Model existing = modelMapper.selectById(currentModelId);
            if (existing != null) {
                return;
            }
            currentModelId = null;
        }

        if (!StringUtils.hasText(modelName)) {
            throw new IllegalArgumentException("车型名称不能为空");
        }

        Long resolvedBrandId = resolveBrandId(brandId, brandName);
        Long modelId = findOrCreateModel(resolvedBrandId, modelName.trim());
        Model model = new Model();
        model.setId(modelId);
        vehicle.setModel(model);
    }

    private Long resolveBrandId(Long brandId, String brandName) {
        if (brandId != null) {
            Brand brand = brandMapper.selectById(brandId);
            if (brand != null) {
                return brandId;
            }
            if (!StringUtils.hasText(brandName)) {
                throw new IllegalArgumentException("品牌不存在，且未提供品牌名称");
            }
        }
        if (!StringUtils.hasText(brandName)) {
            throw new IllegalArgumentException("品牌名称不能为空");
        }
        return findOrCreateBrand(brandName.trim());
    }

    private void ensureCompanyExists(Vehicle vehicle, String companyName) {
        if (vehicle.getCompany() != null && vehicle.getCompany().getId() != null) {
            Company existing = companyMapper.selectById(vehicle.getCompany().getId());
            if (existing != null) {
                return;
            }
            vehicle.setCompany(null);
        }
        if (!StringUtils.hasText(companyName)) {
            throw new IllegalArgumentException("运营公司名称不能为空");
        }
        Long regionId = vehicle.getRegion() != null ? vehicle.getRegion().getId() : null;
        if (regionId == null) {
            throw new IllegalArgumentException("请先填写所属地区");
        }
        Long companyId = findOrCreateCompany(companyName.trim(), regionId);
        Company company = new Company();
        company.setId(companyId);
        vehicle.setCompany(company);
    }

    private Long findOrCreateBrand(String brandName) {
        Brand existing = brandMapper.selectByName(brandName);
        if (existing != null) {
            return existing.getId();
        }
        Brand brand = new Brand();
        brand.setName(brandName);
        brandMapper.insert(brand);
        return brand.getId();
    }

    private Long findOrCreateModel(Long brandId, String modelName) {
        Model existing = modelMapper.selectByBrandAndName(brandId, modelName);
        if (existing != null) {
            return existing.getId();
        }
        Model model = new Model();
        model.setName(modelName);
        Brand brandRef = new Brand();
        brandRef.setId(brandId);
        model.setBrand(brandRef);
        modelMapper.insert(model);
        return model.getId();
    }

    private Long findOrCreateCompany(String companyName, Long regionId) {
        Company existing = companyMapper.selectByName(companyName);
        if (existing != null) {
            return existing.getId();
        }
        Company company = new Company();
        company.setName(companyName);
        Region regionRef = new Region();
        regionRef.setId(regionId);
        company.setRegion(regionRef);
        companyMapper.insert(company);
        return company.getId();
    }

    private Long findOrCreateRegion(String provinceName, String cityName) {
        String cityTrim = cityName.trim();
        Region existingByName = regionMapper.selectByName(cityTrim);
        if (existingByName != null) {
            return existingByName.getId();
        }

        Long provinceId = null;
        String provinceTrim = StringUtils.hasText(provinceName) ? provinceName.trim() : null;
        if (provinceTrim != null) {
            if (provinceTrim.equals(cityTrim)) {
                Region existingProvince = regionMapper.selectByName(provinceTrim);
                if (existingProvince != null) {
                    return existingProvince.getId();
                }
                Region province = new Region();
                province.setName(provinceTrim);
                province.setParentId(null);
                province.setLevel(1);
                regionMapper.insert(province);
                return province.getId();
            }
            Region existingProvince = regionMapper.selectByName(provinceTrim);
            if (existingProvince != null) {
                provinceId = existingProvince.getId();
            } else {
                Region province = new Region();
                province.setName(provinceTrim);
                province.setParentId(null);
                province.setLevel(1);
                regionMapper.insert(province);
                provinceId = province.getId();
            }
        }

        Region existingCity = regionMapper.selectByNameAndParent(cityTrim, provinceId);
        if (existingCity != null) {
            return existingCity.getId();
        }

        Region city = new Region();
        city.setName(cityTrim);
        city.setParentId(provinceId);
        city.setLevel(provinceId == null ? 1 : 2);
        regionMapper.insert(city);
        return city.getId();
    }

    private void upsertVehicleConfig(Vehicle vehicle, VehicleConfig config) {
        if (config == null) {
            vehicleConfigMapper.deleteByVehicleId(vehicle.getId());
            return;
        }
        Vehicle vehicleRef = new Vehicle();
        vehicleRef.setId(vehicle.getId());
        config.setVehicle(vehicleRef);

        VehicleConfig existing = vehicleConfigMapper.selectByVehicleId(vehicle.getId());
        if (existing == null) {
            vehicleConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            vehicleConfigMapper.update(config);
        }
    }

    private void saveVehicleImages(Long vehicleId, List<Long> imageIds) {
        vehicleImageMapper.deleteByVehicleId(vehicleId);
        if (CollectionUtils.isEmpty(imageIds)) {
            return;
        }
        List<VehicleImage> relations = new ArrayList<>(imageIds.size());
        AtomicInteger order = new AtomicInteger(0);
        for (Long imageId : imageIds) {
            VehicleImage relation = new VehicleImage();
            relation.setId(new VehicleImageId(vehicleId, imageId));

            Vehicle vehicleRef = new Vehicle();
            vehicleRef.setId(vehicleId);
            relation.setVehicle(vehicleRef);

            Image imageRef = new Image();
            imageRef.setId(imageId);
            relation.setImage(imageRef);

            relation.setSortOrder(order.getAndIncrement());
            relations.add(relation);
        }
        vehicleImageMapper.insertBatch(relations);
    }
}
