package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.entity.BusRoute;
import com.busgallery.busgallery.entity.Company;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.Model;
import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.entity.VehicleConfig;
import com.busgallery.busgallery.entity.VehicleRoute;
import com.busgallery.busgallery.mapper.BusRouteMapper;
import com.busgallery.busgallery.mapper.VehicleRouteMapper;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.ModelService;
import com.busgallery.busgallery.service.VehicleService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ModelController类用于封装ModelController相关的领域职责（所在包：com.busgallery.busgallery.controller）。
 */
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;
    private final VehicleService vehicleService;
    private final ImageService imageService;
    private final BusRouteMapper busRouteMapper;
    private final VehicleRouteMapper vehicleRouteMapper;

    /**
     * list方法用于处理list相关的业务逻辑。
     * @param brandId brandId参数，详见调用方上下文。
     * @return 返回List<Model>类型结果。
     */
    @GetMapping
    public List<Model> list(@RequestParam(value = "brandId", required = false) Long brandId) {
        if (brandId == null) {
            return modelService.findAll();
        }
        return modelService.findByBrand(brandId);
    }

    /**
     * detail方法用于处理detail相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Model类型结果。
     */
    @GetMapping("/{id}")
    public Model detail(@PathVariable Long id) {
        return modelService.findById(id);
    }

    @GetMapping("/{id}/vehicles")
    public List<VehicleController.VehicleDetailResponse> listVehicles(@PathVariable Long id) {
        List<Vehicle> vehicles = vehicleService.listByModel(id);
        List<VehicleController.VehicleDetailResponse> result = new ArrayList<>(vehicles.size());
        for (Vehicle vehicle : vehicles) {
            VehicleConfig config = vehicleService.findConfigByVehicleId(vehicle.getId());
            List<Image> images = imageService.listByVehicle(vehicle.getId());
            VehicleController.VehicleDetailResponse detail = VehicleController.assembleDetail(vehicle, config, images);

            // Resolve vehicle routes for photo-table grouping
            List<VehicleRoute> vrs = vehicleRouteMapper.selectByVehicleId(vehicle.getId());
            List<VehicleController.RouteBrief> routeBriefs = new ArrayList<>();
            String fallbackRouteNumber = null;

            if (vrs != null && !vrs.isEmpty()) {
                for (VehicleRoute vr : vrs) {
                    if (vr.getRoute() != null) {
                        BusRoute br = vr.getRoute();
                        VehicleController.RouteBrief rb = new VehicleController.RouteBrief();
                        rb.setRouteId(br.getId());
                        rb.setRouteNumber(br.getRouteNumber());
                        rb.setIsCurrent(Boolean.TRUE.equals(vr.getIsCurrent()));
                        routeBriefs.add(rb);
                        // Use first current route as fallback for unlabeled images
                        if (fallbackRouteNumber == null && Boolean.TRUE.equals(vr.getIsCurrent())) {
                            fallbackRouteNumber = br.getRouteNumber();
                        }
                    }
                }
                // Any route as fallback if no current route
                if (fallbackRouteNumber == null && !routeBriefs.isEmpty()) {
                    fallbackRouteNumber = routeBriefs.get(0).getRouteNumber();
                }
            }

            if (detail != null) {
                if (detail.getVehicle() != null) {
                    detail.getVehicle().setRoutes(routeBriefs);
                }
                // Fill route info for images
                if (detail.getImages() != null) {
                    for (VehicleController.ImageDTO imgDto : detail.getImages()) {
                        if (imgDto.getRouteId() != null && !StringUtils.hasText(imgDto.getRouteNumber())) {
                            BusRoute br = busRouteMapper.selectById(imgDto.getRouteId());
                            if (br != null) {
                                imgDto.setRouteNumber(br.getRouteNumber());
                            }
                        }
                        // Fallback: use vehicle's current route for unlabeled images
                        if (!StringUtils.hasText(imgDto.getRouteNumber()) && fallbackRouteNumber != null) {
                            imgDto.setRouteNumber(fallbackRouteNumber);
                        }
                    }
                }
            }
            result.add(detail);
        }
        return result;
    }

    /**
     * 型号分类：展示该型号下所有公司（附缩略图）
     */
    /**
     * listCompanySummaries方法用于处理listCompanySummaries相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回List<CompanySummary>类型结果。
     */
    @GetMapping("/{id}/company-summaries")
    public List<CompanySummary> listCompanySummaries(@PathVariable Long id) {
        List<Vehicle> vehicles = vehicleService.listByModel(id);
        Map<Long, CompanySummary> map = new LinkedHashMap<>();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getCompany() == null) {
                continue;
            }
            Long companyId = vehicle.getCompany().getId();
            CompanySummary summary = map.computeIfAbsent(companyId, key ->
                    new CompanySummary(companyId, vehicle.getCompany().getName(), null));
            if (!StringUtils.hasText(summary.getThumbnailUrl())) {
                List<Image> images = imageService.listByVehicle(vehicle.getId());
                if (!CollectionUtils.isEmpty(images)) {
                    Image img = images.get(0);
                    summary.setThumbnailUrl(StringUtils.hasText(img.getThumbnailUrl()) ? img.getThumbnailUrl() : img.getUrl());
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param model model参数，详见调用方上下文。
     * @return 返回Model类型结果。
     */
    @PostMapping
    public Model create(@RequestBody Model model) {
        return modelService.create(model);
    }

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @param model model参数，详见调用方上下文。
     * @return 返回Model类型结果。
     */
    @PutMapping("/{id}")
    public Model update(@PathVariable Long id, @RequestBody Model model) {
        model.setId(id);
        return modelService.update(model);
    }

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        modelService.delete(id);
    }

    /**
     * CompanySummary类用于封装CompanySummary相关的领域职责（所在包：com.busgallery.busgallery.controller）。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanySummary {
        private Long companyId;
        private String companyName;
        private String thumbnailUrl;
    }
}
