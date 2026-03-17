package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.mapper.*;
import com.busgallery.busgallery.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VehicleServiceImpl类用于封装VehicleServiceImpl相关的领域职责（所在包：com.busgallery.busgallery.service.impl）。
 */
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

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Vehicle类型结果。
     */
    @Override
    public Vehicle findById(Long id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        populateVehicleRelations(vehicle);
        return vehicle;
    }

    /**
     * findByPlateNumber方法用于处理findByPlateNumber相关的业务逻辑。
     * @param plateNumber plateNumber参数，详见调用方上下文。
     * @return 返回Vehicle类型结果。
     */
    @Override
    public Vehicle findByPlateNumber(String plateNumber) {
        Vehicle vehicle = vehicleMapper.selectByPlateNumber(plateNumber);
        populateVehicleRelations(vehicle);
        return vehicle;
    }

    @Override
    public List<Vehicle> listByPlateNumber(String plateNumber) {
        List<Vehicle> vehicles = vehicleMapper.selectAllByPlateNumber(plateNumber);
        if (vehicles != null) {
            vehicles.forEach(this::populateVehicleRelations);
        }
        return vehicles;
    }

    /**
     * listByRegion方法用于处理listByRegion相关的业务逻辑。
     * @param regionId regionId参数，详见调用方上下文。
     * @return 返回List<Vehicle>类型结果。
     */
    @Override
    public List<Vehicle> listByRegion(Long regionId) {
        List<Vehicle> list = vehicleMapper.selectByRegionId(regionId);
        list.forEach(this::populateVehicleRelations);
        return list;
    }

    /**
     * listByCompany方法用于处理listByCompany相关的业务逻辑。
     * @param companyId companyId参数，详见调用方上下文。
     * @return 返回List<Vehicle>类型结果。
     */
    @Override
    public List<Vehicle> listByCompany(Long companyId) {
        List<Vehicle> list = vehicleMapper.selectByCompanyId(companyId);
        list.forEach(this::populateVehicleRelations);
        return list;
    }

    /**
     * listByModel方法用于处理listByModel相关的业务逻辑。
     * @param modelId modelId参数，详见调用方上下文。
     * @return 返回List<Vehicle>类型结果。
     */
    @Override
    public List<Vehicle> listByModel(Long modelId) {
        List<Vehicle> list = vehicleMapper.selectByModelId(modelId);
        list.forEach(this::populateVehicleRelations);
        return list;
    }

    /**
     * queryPage方法用于处理queryPage相关的业务逻辑。
     * @param page page参数，详见调用方上下文。
     * @param size size参数，详见调用方上下文。
     * @param regionId regionId参数，详见调用方上下文。
     * @param companyId companyId参数，详见调用方上下文。
     * @param brandId brandId参数，详见调用方上下文。
     * @param modelId modelId参数，详见调用方上下文。
     * @param keyword keyword参数，详见调用方上下文。
     * @return 返回List<Vehicle>类型结果。
     */
    @Override
    public List<Vehicle> queryPage(int page, int size, Long regionId, Long companyId, Long brandId, Long modelId, String keyword) {
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int offset = (pageNo - 1) * pageSize;
        List<Vehicle> list = vehicleMapper.selectPage(offset, pageSize, regionId, companyId, brandId, modelId, keyword);
        list.forEach(this::populateVehicleRelations);
        return list;
    }

    /**
     * count方法用于处理count相关的业务逻辑。
     * @param regionId regionId参数，详见调用方上下文。
     * @param companyId companyId参数，详见调用方上下文。
     * @param brandId brandId参数，详见调用方上下文。
     * @param modelId modelId参数，详见调用方上下文。
     * @param keyword keyword参数，详见调用方上下文。
     * @return 返回long类型结果。
     */
    @Override
    public long count(Long regionId, Long companyId, Long brandId, Long modelId, String keyword) {
        return vehicleMapper.count(regionId, companyId, brandId, modelId, keyword);
    }

    /**
     * findConfigByVehicleId方法用于处理findConfigByVehicleId相关的业务逻辑。
     * @param vehicleId vehicleId参数，详见调用方上下文。
     * @return 返回VehicleConfig类型结果。
     */
    @Override
    public VehicleConfig findConfigByVehicleId(Long vehicleId) {
        VehicleConfig config = vehicleConfigMapper.selectByVehicleId(vehicleId);
        populateVehicleConfigRelations(config);
        return config;
    }

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param vehicle vehicle参数，详见调用方上下文。
     * @param config config参数，详见调用方上下文。
     * @param imageIds imageIds参数，详见调用方上下文。
     * @param brandId brandId参数，详见调用方上下文。
     * @param brandName brandName参数，详见调用方上下文。
     * @param modelName modelName参数，详见调用方上下文。
     * @param companyName companyName参数，详见调用方上下文。
     * @param regionProvince regionProvince参数，详见调用方上下文。
     * @param regionCity regionCity参数，详见调用方上下文。
     * @return 返回Vehicle类型结果。
     */
    @Override
    @Transactional
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
        return saved;
    }

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param vehicle vehicle参数，详见调用方上下文。
     * @param config config参数，详见调用方上下文。
     * @param imageIds imageIds参数，详见调用方上下文。
     * @param brandId brandId参数，详见调用方上下文。
     * @param brandName brandName参数，详见调用方上下文。
     * @param modelName modelName参数，详见调用方上下文。
     * @param companyName companyName参数，详见调用方上下文。
     * @param regionProvince regionProvince参数，详见调用方上下文。
     * @param regionCity regionCity参数，详见调用方上下文。
     * @return 返回Vehicle类型结果。
     */
    @Override
    @Transactional
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
        return saved;
    }

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        vehicleImageMapper.deleteByVehicleId(id);
        vehicleConfigMapper.deleteByVehicleId(id);
        vehicleMapper.delete(id);
    }

    /**
     * populateVehicleRelations方法用于处理populateVehicleRelations相关的业务逻辑。
     * @param vehicle vehicle参数，详见调用方上下文。
     * @return 无返回值。
     */
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

    /**
     * populateVehicleConfigRelations方法用于处理populateVehicleConfigRelations相关的业务逻辑。
     * @param config config参数，详见调用方上下文。
     * @return 无返回值。
     */
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

    /**
     * populateCompanyRegion方法用于处理populateCompanyRegion相关的业务逻辑。
     * @param company company参数，详见调用方上下文。
     * @return 无返回值。
     */
    private void populateCompanyRegion(Company company) {
        if (company == null) {
            return;
        }
        if (company.getRegion() != null && company.getRegion().getId() != null) {
            Region region = regionMapper.selectById(company.getRegion().getId());
            company.setRegion(region);
        }
    }

    /**
     * ensureRegionExists方法用于处理ensureRegionExists相关的业务逻辑。
     * @param vehicle vehicle参数，详见调用方上下文。
     * @param provinceName provinceName参数，详见调用方上下文。
     * @param cityName cityName参数，详见调用方上下文。
     * @return 无返回值。
     */
    private void ensureRegionExists(Vehicle vehicle, String provinceName, String cityName) {
        if (vehicle.getRegion() != null && vehicle.getRegion().getId() != null) {
            return;
        }
        if (!StringUtils.hasText(cityName)) {
            throw new IllegalArgumentException("请选择城市或填写地区信息");
        }
        Long regionId = findOrCreateRegion(provinceName, cityName.trim());
        Region region = new Region();
        region.setId(regionId);
        vehicle.setRegion(region);
    }

    /**
     * ensureModelExists方法用于处理ensureModelExists相关的业务逻辑。
     * @param vehicle vehicle参数，详见调用方上下文。
     * @param brandId brandId参数，详见调用方上下文。
     * @param brandName brandName参数，详见调用方上下文。
     * @param modelName modelName参数，详见调用方上下文。
     * @return 无返回值。
     */
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
            throw new IllegalArgumentException("请选择或填写车型名称");
        }

        Long resolvedBrandId = resolveBrandId(brandId, brandName);
        Long modelId = findOrCreateModel(resolvedBrandId, modelName.trim());
        Model model = new Model();
        model.setId(modelId);
        vehicle.setModel(model);
    }

    /**
     * resolveBrandId方法用于处理resolveBrandId相关的业务逻辑。
     * @param brandId brandId参数，详见调用方上下文。
     * @param brandName brandName参数，详见调用方上下文。
     * @return 返回Long类型结果。
     */
    private Long resolveBrandId(Long brandId, String brandName) {
        if (brandId != null) {
            Brand brand = brandMapper.selectById(brandId);
            if (brand != null) {
                return brandId;
            }
            if (!StringUtils.hasText(brandName)) {
                throw new IllegalArgumentException("原有品牌不存在，请重新选择或填写品牌名称");
            }
        }
        if (!StringUtils.hasText(brandName)) {
            throw new IllegalArgumentException("请提供品牌信息");
        }
        return findOrCreateBrand(brandName.trim());
    }

    /**
     * ensureCompanyExists方法用于处理ensureCompanyExists相关的业务逻辑。
     * @param vehicle vehicle参数，详见调用方上下文。
     * @param companyName companyName参数，详见调用方上下文。
     * @return 无返回值。
     */
    private void ensureCompanyExists(Vehicle vehicle, String companyName) {
        if (vehicle.getCompany() != null && vehicle.getCompany().getId() != null) {
            Company existing = companyMapper.selectById(vehicle.getCompany().getId());
            if (existing != null) {
                return;
            }
            vehicle.setCompany(null);
        }
        if (!StringUtils.hasText(companyName)) {
            throw new IllegalArgumentException("公司ID或公司名称至少需要提供一个");
        }
        Long regionId = vehicle.getRegion() != null ? vehicle.getRegion().getId() : null;
        if (regionId == null) {
            throw new IllegalArgumentException("创建新公司时必须选择地区");
        }
        Long companyId = findOrCreateCompany(companyName.trim(), regionId);
        Company company = new Company();
        company.setId(companyId);
        vehicle.setCompany(company);
    }

    /**
     * findOrCreateBrand方法用于处理findOrCreateBrand相关的业务逻辑。
     * @param brandName brandName参数，详见调用方上下文。
     * @return 返回Long类型结果。
     */
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

    /**
     * findOrCreateModel方法用于处理findOrCreateModel相关的业务逻辑。
     * @param brandId brandId参数，详见调用方上下文。
     * @param modelName modelName参数，详见调用方上下文。
     * @return 返回Long类型结果。
     */
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

    /**
     * findOrCreateCompany方法用于处理findOrCreateCompany相关的业务逻辑。
     * @param companyName companyName参数，详见调用方上下文。
     * @param regionId regionId参数，详见调用方上下文。
     * @return 返回Long类型结果。
     */
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

    /**
     * findOrCreateRegion方法用于处理findOrCreateRegion相关的业务逻辑。
     * @param provinceName provinceName参数，详见调用方上下文。
     * @param cityName cityName参数，详见调用方上下文。
     * @return 返回Long类型结果。
     */
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

    /**
     * upsertVehicleConfig方法用于处理upsertVehicleConfig相关的业务逻辑。
     * @param vehicle vehicle参数，详见调用方上下文。
     * @param config config参数，详见调用方上下文。
     * @return 无返回值。
     */
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

    /**
     * saveVehicleImages方法用于处理saveVehicleImages相关的业务逻辑。
     * @param vehicleId vehicleId参数，详见调用方上下文。
     * @param imageIds imageIds参数，详见调用方上下文。
     * @return 无返回值。
     */
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
