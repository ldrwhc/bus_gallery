package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.dto.vehicle.ModelSummaryRow;
import com.busgallery.busgallery.mapper.*;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    private static final int MAX_PAGE_SIZE = 30;
    private static final int VEHICLE_IMAGE_DEADLOCK_MAX_RETRIES = 5;
    private static final long VEHICLE_IMAGE_RETRY_BASE_DELAY_MS = 50L;

    private final VehicleMapper vehicleMapper;
    private final VehicleConfigMapper vehicleConfigMapper;
    private final VehicleImageMapper vehicleImageMapper;
    private final VehicleFavoriteMapper vehicleFavoriteMapper;
    private final VehicleCommentMapper vehicleCommentMapper;
    private final BrandMapper brandMapper;
    private final ModelMapper modelMapper;
    private final CompanyMapper companyMapper;
    private final RegionMapper regionMapper;
    private final ImageService imageService;
    private final StringRedisTemplate stringRedisTemplate;

    public Vehicle findById(Long id) {
        Vehicle vehicle = vehicleMapper.selectDetailById(id);
        if (vehicle == null) {
            return null;
        }
        return vehicle;
    }

    public Vehicle findByPlateNumber(String plateNumber) {
        Vehicle vehicle = vehicleMapper.selectByPlateNumber(plateNumber);
        populateVehicleRelations(vehicle);
        return vehicle;
    }

    public List<Vehicle> listByPlateNumber(String plateNumber) {
        List<Vehicle> vehicles = vehicleMapper.selectDetailByPlateNumber(plateNumber);
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

    @Override
    public List<ModelSummaryRow> listModelSummariesByCompany(Long companyId) {
        if (companyId == null) {
            return List.of();
        }
        return vehicleMapper.selectModelSummariesByCompany(companyId);
    }

    public List<Vehicle> queryPage(int size, Long regionId, Long companyId, Long brandId, Long modelId, String keyword, java.time.LocalDate lastLaunch, Long lastId) {
        int pageSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        List<Vehicle> list = vehicleMapper.selectPageByCursor(pageSize, regionId, companyId, brandId, modelId, keyword, lastLaunch, lastId);
        list.forEach(this::populateVehicleRelations);
        return list;
    }

    @Override
    public Vehicle findRandomByCompanyAndModel(Long companyId, Long modelId) {
        if (companyId == null || modelId == null) {
            return null;
        }
        Vehicle vehicle = vehicleMapper.selectRandomByCompanyAndModel(companyId, modelId);
        populateVehicleRelations(vehicle);
        return vehicle;
    }

    @Override
    public List<Vehicle> listHotByViewCount(int limit) {
        int size = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        List<Vehicle> list = vehicleMapper.selectHotByViewCount(size);
        list.forEach(this::populateVehicleRelations);
        return list;
    }

    public long count(Long regionId, Long companyId, Long brandId, Long modelId, String keyword) {
        return vehicleMapper.count(regionId, companyId, brandId, modelId, keyword);
    }

    @Override
    public void recordView(Long vehicleId, String clientFingerprint) {
        if (vehicleId == null) {
            return;
        }
        if (!isViewCountAllowed(vehicleId, clientFingerprint)) {
            return;
        }
        vehicleMapper.incrementViewCount(vehicleId, 1L);
    }

    public VehicleConfig findConfigByVehicleId(Long vehicleId) {
        VehicleConfig config = vehicleConfigMapper.selectByVehicleId(vehicleId);
        populateVehicleConfigRelations(config);
        return config;
    }

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
        saveVehicleImages(vehicle.getId(), imageIds, false);
        Vehicle saved = vehicleMapper.selectById(vehicle.getId());
        populateVehicleRelations(saved);
        bumpVehiclePageVersion();
        invalidateVehicleSnapshots(saved != null ? saved.getPlateNumber() : null);
        return saved;
    }

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
        Vehicle existing = vehicle != null && vehicle.getId() != null ? vehicleMapper.selectById(vehicle.getId()) : null;
        ensureRegionExists(vehicle, regionProvince, regionCity);
        ensureModelExists(vehicle, brandId, brandName, modelName);
        ensureCompanyExists(vehicle, companyName);
        vehicleMapper.update(vehicle);
        upsertVehicleConfig(vehicle, config);
        saveVehicleImages(vehicle.getId(), imageIds, true);
        Vehicle saved = vehicleMapper.selectById(vehicle.getId());
        populateVehicleRelations(saved);
        bumpVehiclePageVersion();
        invalidateVehicleSnapshots(
                existing != null ? existing.getPlateNumber() : null,
                saved != null ? saved.getPlateNumber() : null
        );
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Vehicle existing = vehicleMapper.selectById(id);
        List<VehicleImage> relations = vehicleImageMapper.selectByVehicleId(id);
        vehicleFavoriteMapper.deleteByVehicleId(id);
        vehicleCommentMapper.deleteByVehicleId(id);
        for (VehicleImage relation : relations) {
            Long imageId = relation != null && relation.getImage() != null ? relation.getImage().getId() : null;
            if (imageId == null) {
                continue;
            }
            if (vehicleImageMapper.countByImageId(imageId) <= 1) {
                imageService.delete(imageId);
            }
        }
        vehicleImageMapper.deleteByVehicleId(id);
        vehicleConfigMapper.deleteByVehicleId(id);
        vehicleMapper.delete(id);
        bumpVehiclePageVersion();
        invalidateVehicleSnapshots(existing != null ? existing.getPlateNumber() : null);
    }

    private void bumpVehiclePageVersion() {
        try {
            stringRedisTemplate.opsForValue().increment("bg:vehicle:page:version");
        } catch (Exception ignore) {
        }
    }

    private void invalidateVehicleSnapshots(String... plateNumbers) {
        if (plateNumbers == null || plateNumbers.length == 0) {
            return;
        }
        for (String plateNumber : plateNumbers) {
            String normalizedPlate = normalizePlate(plateNumber);
            if (!StringUtils.hasText(normalizedPlate)) {
                continue;
            }
            String latestKey = buildSnapshotLatestKey(normalizedPlate);
            Set<String> keysToDelete = new LinkedHashSet<>();
            keysToDelete.add(latestKey);
            keysToDelete.add(buildSnapshotStaleKey(normalizedPlate));
            keysToDelete.add(buildSnapshotLockKey(normalizedPlate));
            try {
                String version = stringRedisTemplate.opsForValue().get(latestKey);
                if (StringUtils.hasText(version)) {
                    keysToDelete.add(buildSnapshotVersionKey(normalizedPlate, version));
                }
                stringRedisTemplate.delete(keysToDelete);
            } catch (Exception ignore) {
            }
        }
    }

    private String normalizePlate(String plateNumber) {
        return plateNumber == null ? null : plateNumber.replaceAll("\\s+", "");
    }

    private String buildSnapshotLatestKey(String normalizedPlate) {
        return "bg:snapshot:plate:" + normalizedPlate + ":latest";
    }

    private String buildSnapshotVersionKey(String normalizedPlate, String version) {
        return "bg:snapshot:plate:" + normalizedPlate + ":v" + version;
    }

    private String buildSnapshotStaleKey(String normalizedPlate) {
        return "bg:snapshot:plate:" + normalizedPlate + ":stale";
    }

    private String buildSnapshotLockKey(String normalizedPlate) {
        return "bg:snapshot:plate:" + normalizedPlate + ":lock";
    }

    private boolean isViewCountAllowed(Long vehicleId, String clientFingerprint) {
        String fingerprint = StringUtils.hasText(clientFingerprint) ? clientFingerprint.trim() : "unknown";
        String digest = DigestUtils.md5DigestAsHex(fingerprint.getBytes(StandardCharsets.UTF_8));
        String key = "bg:vehicle:view:dedupe:" + vehicleId + ":" + digest;
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(20));
            return Boolean.TRUE.equals(ok);
        } catch (Exception ignore) {
            return true;
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

        Long provinceId = null;
        String provinceTrim = StringUtils.hasText(provinceName) ? provinceName.trim() : null;
        if (provinceTrim != null) {
            if (provinceTrim.equals(cityTrim)) {
                Region existingProvince = regionMapper.selectProvinceByName(provinceTrim);
                if (existingProvince != null) {
                    return existingProvince.getId();
                }
                return createProvince(provinceTrim);
            }
            Region existingProvince = regionMapper.selectProvinceByName(provinceTrim);
            if (existingProvince != null) {
                provinceId = existingProvince.getId();
            } else {
                provinceId = createProvince(provinceTrim);
            }
        }

        Region existingCity = regionMapper.selectCityByNameAndProvince(cityTrim, provinceId);
        if (existingCity == null) {
            existingCity = regionMapper.selectByNameAndParent(cityTrim, provinceId);
        }
        if (existingCity != null) {
            return existingCity.getId();
        }

        if (provinceId == null) {
            return createProvince(cityTrim);
        }
        return createCity(provinceId, cityTrim);
    }

    private Long createProvince(String provinceName) {
        Region province = new Region();
        province.setName(provinceName);
        province.setParentId(null);
        province.setLevel(1);
        province.setRegionType("PROVINCE");
        regionMapper.insert(province);
        province.setProvinceId(province.getId());
        regionMapper.update(province);
        return province.getId();
    }

    private Long createCity(Long provinceId, String cityName) {
        Region city = new Region();
        city.setName(cityName);
        city.setParentId(provinceId);
        city.setProvinceId(provinceId);
        city.setLevel(2);
        city.setRegionType("CITY");
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

    private void saveVehicleImages(Long vehicleId, List<Long> imageIds, boolean replaceExisting) {
        if (vehicleId == null) {
            return;
        }
        List<Long> orderedImageIds = normalizeImageIds(imageIds);
        Map<Long, Integer> originalSortOrder = buildImageSortOrder(orderedImageIds);
        List<Long> sortedImageIds = new ArrayList<>(orderedImageIds);
        sortedImageIds.sort(Long::compareTo);

        executeVehicleImageWriteWithRetry(() -> {
            if (replaceExisting) {
                vehicleImageMapper.deleteByVehicleId(vehicleId);
            }
            if (sortedImageIds.isEmpty()) {
                return;
            }
            List<VehicleImage> relations = new ArrayList<>(sortedImageIds.size());
            for (Long imageId : sortedImageIds) {
                VehicleImage relation = new VehicleImage();
                relation.setId(new VehicleImageId(vehicleId, imageId));

                Vehicle vehicleRef = new Vehicle();
                vehicleRef.setId(vehicleId);
                relation.setVehicle(vehicleRef);

                Image imageRef = new Image();
                imageRef.setId(imageId);
                relation.setImage(imageRef);

                relation.setSortOrder(originalSortOrder.getOrDefault(imageId, 0));
                relations.add(relation);
            }
            vehicleImageMapper.insertBatch(relations);
        }, vehicleId, sortedImageIds.size(), replaceExisting);
    }

    private List<Long> normalizeImageIds(List<Long> imageIds) {
        if (CollectionUtils.isEmpty(imageIds)) {
            return List.of();
        }
        List<Long> normalized = new ArrayList<>(imageIds.size());
        Set<Long> dedupe = new LinkedHashSet<>();
        for (Long imageId : imageIds) {
            if (imageId == null) {
                continue;
            }
            if (dedupe.add(imageId)) {
                normalized.add(imageId);
            }
        }
        return normalized;
    }

    private Map<Long, Integer> buildImageSortOrder(List<Long> orderedImageIds) {
        Map<Long, Integer> sortOrder = new LinkedHashMap<>();
        AtomicInteger index = new AtomicInteger(0);
        orderedImageIds.forEach(imageId -> sortOrder.putIfAbsent(imageId, index.getAndIncrement()));
        return sortOrder;
    }

    private void executeVehicleImageWriteWithRetry(Runnable writer,
                                                   Long vehicleId,
                                                   int imageCount,
                                                   boolean replaceExisting) {
        int attempt = 0;
        while (true) {
            try {
                writer.run();
                return;
            } catch (RuntimeException ex) {
                if (!isDeadlockException(ex) || attempt >= VEHICLE_IMAGE_DEADLOCK_MAX_RETRIES - 1) {
                    throw ex;
                }
                long delayMs = computeBackoffMs(attempt);
                log.warn("Deadlock on vehicle_image write, retrying: vehicleId={}, imageCount={}, replaceExisting={}, attempt={}, waitMs={}",
                        vehicleId, imageCount, replaceExisting, attempt + 1, delayMs);
                sleepQuietly(delayMs);
                attempt++;
            }
        }
    }

    private boolean isDeadlockException(Throwable ex) {
        if (ex instanceof DeadlockLoserDataAccessException
                || ex instanceof CannotAcquireLockException
                || ex instanceof PessimisticLockingFailureException) {
            return true;
        }
        Throwable cursor = ex;
        while (cursor != null) {
            if (cursor instanceof SQLException sqlException) {
                if (sqlException.getErrorCode() == 1213 || "40001".equals(sqlException.getSQLState())) {
                    return true;
                }
            }
            String message = cursor.getMessage();
            if (StringUtils.hasText(message)) {
                String normalized = message.toLowerCase();
                if (normalized.contains("deadlock found when trying to get lock")
                        || normalized.contains("sqlstate: 40001")
                        || normalized.contains("error 1213")) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private long computeBackoffMs(int attempt) {
        long base = (long) (VEHICLE_IMAGE_RETRY_BASE_DELAY_MS * Math.pow(2, attempt));
        long jitter = ThreadLocalRandom.current().nextLong(20L, 80L);
        return base + jitter;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
