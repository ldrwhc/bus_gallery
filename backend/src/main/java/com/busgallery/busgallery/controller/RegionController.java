package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.entity.Company;
import com.busgallery.busgallery.entity.Region;
import com.busgallery.busgallery.service.CompanyService;
import com.busgallery.busgallery.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RegionController类用于封装RegionController相关的领域职责（所在包：com.busgallery.busgallery.controller）。
 */
@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;
    private final CompanyService companyService;

    /**
     * list方法用于处理list相关的业务逻辑。
     * @param parentId parentId参数，详见调用方上下文。
     * @return 返回List<Region>类型结果。
     */
    @GetMapping
    public List<Region> list(@RequestParam(value = "parentId", required = false) Long parentId) {
        if (parentId == null) {
            return regionService.findAll();
        }
        return regionService.findChildren(parentId);
    }

    /**
     * detail方法用于处理detail相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Region类型结果。
     */
    @GetMapping("/{id}")
    public Region detail(@PathVariable Long id) {
        return regionService.findById(id);
    }

    /**
     * 地区分类：显示该地区下所有公司
     */
    /**
     * listCompanies方法用于处理listCompanies相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回List<Company>类型结果。
     */
    @GetMapping("/{id}/companies")
    public List<Company> listCompanies(@PathVariable Long id) {
        return companyService.findByRegion(id);
    }

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param region region参数，详见调用方上下文。
     * @return 返回Region类型结果。
     */
    @PostMapping
    public Region create(@RequestBody Region region) {
        return regionService.create(region);
    }

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @param region region参数，详见调用方上下文。
     * @return 返回Region类型结果。
     */
    @PutMapping("/{id}")
    public Region update(@PathVariable Long id, @RequestBody Region region) {
        region.setId(id);
        return regionService.update(region);
    }

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        regionService.delete(id);
    }
}