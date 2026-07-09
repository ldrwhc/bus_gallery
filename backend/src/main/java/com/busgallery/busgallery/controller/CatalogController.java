package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.service.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CatalogController类用于封装CatalogController相关的领域职责（所在包：com.busgallery.busgallery.controller）。
 */
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final RegionService regionService;
    private final CompanyService companyService;
    private final BrandService brandService;
    private final ModelService modelService;
    private final VehicleService vehicleService;
    private final ImageService imageService;

    /**
     * regions方法用于处理regions相关的业务逻辑。
     * @return 返回List<RegionCatalogItem>类型结果。
     */
    @GetMapping("/regions")
    public List<RegionCatalogItem> regions() {
        List<Region> regions = regionService.findAll();
        Map<Long, Region> regionMap = regions.stream()
                .collect(Collectors.toMap(Region::getId, r -> r, (a, b) -> a, LinkedHashMap::new));
        List<Company> companies = companyService.findAll();
        Map<Long, List<Company>> grouped = new LinkedHashMap<>();
        for (Company company : companies) {
            Long regionId = company.getRegion() != null ? company.getRegion().getId() : null;
            grouped.computeIfAbsent(regionId, key -> new ArrayList<>()).add(company);
        }
        List<RegionCatalogItem> result = new ArrayList<>();
        for (Region region : regionMap.values()) {
            List<Company> regionCompanies = grouped.getOrDefault(region.getId(), Collections.emptyList());
            List<RegionCompanySummary> summaries = new ArrayList<>();
            for (Company company : regionCompanies) {
                summaries.add(buildCompanySummary(company, region.getName()));
            }
            result.add(new RegionCatalogItem(region.getId(), region.getName(), summaries));
        }
        return result;
    }

    /**
     * companies方法用于处理companies相关的业务逻辑。
     * @return 返回List<CompanyCatalogItem>类型结果。
     */
    @GetMapping("/companies")
    public List<CompanyCatalogItem> companies() {
        List<Company> companies = companyService.findAll();
        Map<Long, Region> regionMap = regionService.findAll().stream()
                .collect(Collectors.toMap(Region::getId, r -> r, (a, b) -> a));
        List<CompanyCatalogItem> result = new ArrayList<>();
        for (Company company : companies) {
            List<Vehicle> vehicles = vehicleService.listByCompany(company.getId());
            Map<String, CompanyModelSummary> models = new LinkedHashMap<>();
            for (Vehicle vehicle : vehicles) {
                if (vehicle.getModel() == null) {
                    continue;
                }
                Long modelId = vehicle.getModel().getId();
                Integer batchYear = vehicle.getLaunchDate() != null ? vehicle.getLaunchDate().getYear() : null;
                String batchKey = modelId + "_" + (batchYear != null ? batchYear : "unknown");
                CompanyModelSummary summary = models.computeIfAbsent(batchKey, key ->
                        new CompanyModelSummary(
                                modelId,
                                vehicle.getModel().getName(),
                                vehicle.getModel().getBrand() != null ? vehicle.getModel().getBrand().getName() : null,
                                null,
                                batchYear,
                                0
                        ));
                summary.setVehicleCount(summary.getVehicleCount() + 1);
                if (!StringUtils.hasText(summary.getThumbnailUrl())) {
                    summary.setThumbnailUrl(resolveThumbnail(Collections.singletonList(vehicle)));
                }
            }
            String regionName = null;
            if (company.getRegion() != null && company.getRegion().getId() != null) {
                Region region = regionMap.get(company.getRegion().getId());
                regionName = region != null ? region.getName() : null;
            }
            result.add(new CompanyCatalogItem(
                    company.getId(),
                    company.getName(),
                    regionName,
                    new ArrayList<>(models.values())
            ));
        }
        return result;
    }

    /**
     * brands方法用于处理brands相关的业务逻辑。
     * @return 返回List<BrandCatalogItem>类型结果。
     */
    @GetMapping("/brands")
    public List<BrandCatalogItem> brands() {
        List<Brand> brands = brandService.findAll();
        List<BrandCatalogItem> result = new ArrayList<>();
        for (Brand brand : brands) {
            List<Model> models = modelService.findByBrand(brand.getId());
            List<ModelSummary> summaries = new ArrayList<>();
            for (Model model : models) {
                List<Vehicle> vehicles = vehicleService.listByModel(model.getId());
                String thumbnail = resolveThumbnail(vehicles);
                summaries.add(new ModelSummary(model.getId(), model.getName(), thumbnail));
            }
            result.add(new BrandCatalogItem(brand.getId(), brand.getName(), summaries));
        }
        return result;
    }

    /**
     * models方法用于处理models相关的业务逻辑。
     * @return 返回List<ModelCatalogItem>类型结果。
     */
    @GetMapping("/models")
    public List<ModelCatalogItem> models() {
        List<Model> models = modelService.findAll();
        List<ModelCatalogItem> result = new ArrayList<>();
        for (Model model : models) {
            List<Vehicle> vehicles = vehicleService.listByModel(model.getId());
            Map<Long, ModelCompanySummary> companyMap = new LinkedHashMap<>();
            for (Vehicle vehicle : vehicles) {
                if (vehicle.getCompany() == null) {
                    continue;
                }
                Company company = vehicle.getCompany();
                Long companyId = company.getId();
                ModelCompanySummary summary = companyMap.computeIfAbsent(companyId, id ->
                        new ModelCompanySummary(
                                companyId,
                                company.getName(),
                                company.getRegion() != null ? company.getRegion().getId() : null,
                                company.getRegion() != null ? company.getRegion().getName() : null,
                                null
                        ));
                if (!StringUtils.hasText(summary.getThumbnailUrl())) {
                    summary.setThumbnailUrl(resolveThumbnail(Collections.singletonList(vehicle)));
                }
            }
            result.add(new ModelCatalogItem(
                    model.getId(),
                    model.getName(),
                    model.getBrand() != null ? model.getBrand().getName() : null,
                    new ArrayList<>(companyMap.values())
            ));
        }
        return result;
    }

    /**
     * buildCompanySummary方法用于处理buildCompanySummary相关的业务逻辑。
     * @param company company参数，详见调用方上下文。
     * @param regionName regionName参数，详见调用方上下文。
     * @return 返回RegionCompanySummary类型结果。
     */
    private RegionCompanySummary buildCompanySummary(Company company, String regionName) {
        List<Vehicle> vehicles = vehicleService.listByCompany(company.getId());
        Set<Long> modelIds = new LinkedHashSet<>();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getModel() != null && vehicle.getModel().getId() != null) {
                modelIds.add(vehicle.getModel().getId());
            }
        }
        String thumbnail = resolveThumbnail(vehicles);
        return new RegionCompanySummary(
                company.getId(),
                company.getName(),
                regionName,
                thumbnail,
                modelIds.size()
        );
    }

    /**
     * resolveThumbnail方法用于处理resolveThumbnail相关的业务逻辑。
     * @param vehicles vehicles参数，详见调用方上下文。
     * @return 返回String类型结果。
     */
    private String resolveThumbnail(List<Vehicle> vehicles) {
        if (CollectionUtils.isEmpty(vehicles)) {
            return null;
        }
        for (Vehicle vehicle : vehicles) {
            List<Image> images = imageService.listByVehicle(vehicle.getId());
            if (!CollectionUtils.isEmpty(images)) {
                Image img = images.get(0);
                if (StringUtils.hasText(img.getThumbnailUrl())) {
                    return img.getThumbnailUrl();
                }
                if (StringUtils.hasText(img.getUrl())) {
                    return img.getUrl();
                }
            }
        }
        return null;
    }

    /**
     * RegionCatalogItem类用于封装RegionCatalogItem相关的领域职责（所在包：com.busgallery.busgallery.controller）。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegionCatalogItem {
        private Long id;
        private String name;
        private List<RegionCompanySummary> companies;
    }

    /**
     * RegionCompanySummary类用于封装RegionCompanySummary相关的领域职责（所在包：com.busgallery.busgallery.controller）。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegionCompanySummary {
        private Long id;
        private String name;
        private String regionName;
        private String thumbnailUrl;
        private int modelsCount;
    }

    /**
     * CompanyCatalogItem类用于封装CompanyCatalogItem相关的领域职责（所在包：com.busgallery.busgallery.controller）。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompanyCatalogItem {
        private Long id;
        private String name;
        private String regionName;
        private List<CompanyModelSummary> models;
    }

    /**
     * CompanyModelSummary类用于封装CompanyModelSummary相关的领域职责（所在包：com.busgallery.busgallery.controller）。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompanyModelSummary {
        private Long id;
        private String name;
        private String brandName;
        private String thumbnailUrl;
        private Integer batchYear;
        private int vehicleCount;
    }

    /**
     * BrandCatalogItem类用于封装BrandCatalogItem相关的领域职责（所在包：com.busgallery.busgallery.controller）。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BrandCatalogItem {
        private Long id;
        private String name;
        private List<ModelSummary> models;
    }

    /**
     * ModelSummary类用于封装ModelSummary相关的领域职责（所在包：com.busgallery.busgallery.controller）。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ModelSummary {
        private Long id;
        private String name;
        private String thumbnailUrl;
    }

    /**
     * ModelCatalogItem类用于封装ModelCatalogItem相关的领域职责（所在包：com.busgallery.busgallery.controller）。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ModelCatalogItem {
        private Long id;
        private String name;
        private String brandName;
        private List<ModelCompanySummary> companies;
    }

    /**
     * ModelCompanySummary类用于封装ModelCompanySummary相关的领域职责（所在包：com.busgallery.busgallery.controller）。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ModelCompanySummary {
        private Long id;
        private String name;
        private Long regionId;
        private String regionName;
        private String thumbnailUrl;
    }
}
