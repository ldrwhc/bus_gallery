package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.entity.Company;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.entity.VehicleConfig;
import com.busgallery.busgallery.dto.vehicle.ModelSummaryRow;
import com.busgallery.busgallery.service.CompanyService;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.VehicleService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * CompanyController类用于封装CompanyController相关的领域职责（所在包：com.busgallery.busgallery.controller）。
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final VehicleService vehicleService;
    private final ImageService imageService;

    /**
     * list方法用于处理list相关的业务逻辑。
     * @param regionId regionId参数，详见调用方上下文。
     * @return 返回List<Company>类型结果。
     */
    @GetMapping
    public List<Company> list(@RequestParam(value = "regionId", required = false) Long regionId) {
        if (regionId == null) {
            return companyService.findAll();
        }
        return companyService.findByRegion(regionId);
    }

    /**
     * detail方法用于处理detail相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Company类型结果。
     */
    @GetMapping("/{id}")
    public Company detail(@PathVariable Long id) {
        return companyService.findById(id);
    }

    @GetMapping("/{id}/vehicles")
    public List<VehicleController.VehicleDetailResponse> listVehicles(@PathVariable Long id) {
        List<Vehicle> vehicles = vehicleService.listByCompany(id);
        List<VehicleController.VehicleDetailResponse> result = new ArrayList<>(vehicles.size());
        for (Vehicle vehicle : vehicles) {
            VehicleConfig config = vehicleService.findConfigByVehicleId(vehicle.getId());
            List<Image> images = imageService.listByVehicle(vehicle.getId());
            result.add(VehicleController.assembleDetail(vehicle, config, images));
        }
        return result;
    }

    /**
     * 公司分类：展示该公司所有车型（附缩略图）
     */
    /**
     * listModelSummaries方法用于处理listModelSummaries相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回List<ModelSummary>类型结果。
     */
    @GetMapping("/{id}/model-summaries")
    public List<ModelSummary> listModelSummaries(@PathVariable Long id) {
        List<ModelSummaryRow> rows = vehicleService.listModelSummariesByCompany(id);
        return rows.stream()
                .map(row -> new ModelSummary(row.getModelId(), row.getModelName(), null))
                .toList();
    }

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param company company参数，详见调用方上下文。
     * @return 返回Company类型结果。
     */
    @PostMapping
    public Company create(@RequestBody Company company) {
        return companyService.create(company);
    }

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @param company company参数，详见调用方上下文。
     * @return 返回Company类型结果。
     */
    @PutMapping("/{id}")
    public Company update(@PathVariable Long id, @RequestBody Company company) {
        company.setId(id);
        return companyService.update(company);
    }

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        companyService.delete(id);
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
