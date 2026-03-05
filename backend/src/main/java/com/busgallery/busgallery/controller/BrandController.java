package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.Model;
import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.service.BrandService;
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
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;
    private final ModelService modelService;
    private final VehicleService vehicleService;
    private final ImageService imageService;

    @GetMapping
    public Object list() {
        return brandService.findAll();
    }

    @GetMapping("/{id}")
    public Object detail(@PathVariable Long id) {
        return brandService.findById(id);
    }

    @GetMapping("/{id}/models")
    public List<Model> listModels(@PathVariable Long id) {
        return modelService.findByBrand(id);
    }

    /**
     * 品牌分类：该品牌下所有车型（附缩略图）
     */
    @GetMapping("/{id}/model-summaries")
    public List<ModelSummary> listModelSummaries(@PathVariable Long id) {
        List<Model> models = modelService.findByBrand(id);
        List<ModelSummary> summaries = new ArrayList<>(models.size());
        for (Model model : models) {
            String thumbnail = resolveModelThumbnail(model.getId());
            summaries.add(new ModelSummary(model.getId(), model.getName(), thumbnail));
        }
        return summaries;
    }

    @PostMapping
    public Object create(@RequestBody Model brand) {
        return brandService.create(brand.getBrand());
    }

    @PutMapping("/{id}")
    public Object update(@PathVariable Long id, @RequestBody Model brand) {
        brand.setId(id);
        return brandService.update(brand.getBrand());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        brandService.delete(id);
    }

    private String resolveModelThumbnail(Long modelId) {
        List<Vehicle> vehicles = vehicleService.listByModel(modelId);
        for (Vehicle vehicle : vehicles) {
            List<Image> images = imageService.listByVehicle(vehicle.getId());
            if (!CollectionUtils.isEmpty(images)) {
                Image img = images.get(0);
                return StringUtils.hasText(img.getThumbnailUrl()) ? img.getThumbnailUrl() : img.getUrl();
            }
        }
        return null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelSummary {
        private Long modelId;
        private String modelName;
        private String thumbnailUrl;
    }
}