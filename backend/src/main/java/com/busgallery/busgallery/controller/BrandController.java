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

/**
 * BrandController类用于封装BrandController相关的领域职责（所在包：com.busgallery.busgallery.controller）。
 */
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;
    private final ModelService modelService;
    private final VehicleService vehicleService;
    private final ImageService imageService;

    /**
     * list方法用于处理list相关的业务逻辑。
     * @return 返回Object类型结果。
     */
    @GetMapping
    public Object list() {
        return brandService.findAll();
    }

    /**
     * detail方法用于处理detail相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Object类型结果。
     */
    @GetMapping("/{id}")
    public Object detail(@PathVariable Long id) {
        return brandService.findById(id);
    }

    /**
     * listModels方法用于处理listModels相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回List<Model>类型结果。
     */
    @GetMapping("/{id}/models")
    public List<Model> listModels(@PathVariable Long id) {
        return modelService.findByBrand(id);
    }

    /**
     * 品牌分类：该品牌下所有车型（附缩略图）
     */
    /**
     * listModelSummaries方法用于处理listModelSummaries相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回List<ModelSummary>类型结果。
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

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param brand brand参数，详见调用方上下文。
     * @return 返回Object类型结果。
     */
    @PostMapping
    public Object create(@RequestBody Model brand) {
        return brandService.create(brand.getBrand());
    }

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @param brand brand参数，详见调用方上下文。
     * @return 返回Object类型结果。
     */
    @PutMapping("/{id}")
    public Object update(@PathVariable Long id, @RequestBody Model brand) {
        brand.setId(id);
        return brandService.update(brand.getBrand());
    }

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        brandService.delete(id);
    }

    /**
     * resolveModelThumbnail方法用于处理resolveModelThumbnail相关的业务逻辑。
     * @param modelId modelId参数，详见调用方上下文。
     * @return 返回String类型结果。
     */
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

    /**
     * ModelSummary类用于封装ModelSummary相关的领域职责（所在包：com.busgallery.busgallery.controller）。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelSummary {
        private Long modelId;
        private String modelName;
        private String thumbnailUrl;
    }
}