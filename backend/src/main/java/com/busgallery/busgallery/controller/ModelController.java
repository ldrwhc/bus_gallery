package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.entity.Company;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.Model;
import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.entity.VehicleConfig;
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

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;
    private final VehicleService vehicleService;
    private final ImageService imageService;

    @GetMapping
    public List<Model> list(@RequestParam(value = "brandId", required = false) Long brandId) {
        if (brandId == null) {
            return modelService.findAll();
        }
        return modelService.findByBrand(brandId);
    }

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
            result.add(VehicleController.assembleDetail(vehicle, config, images));
        }
        return result;
    }

    /**
     * 型号分类：展示该型号下所有公司（附缩略图）
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

    @PostMapping
    public Model create(@RequestBody Model model) {
        return modelService.create(model);
    }

    @PutMapping("/{id}")
    public Model update(@PathVariable Long id, @RequestBody Model model) {
        model.setId(id);
        return modelService.update(model);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        modelService.delete(id);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanySummary {
        private Long companyId;
        private String companyName;
        private String thumbnailUrl;
    }
}
