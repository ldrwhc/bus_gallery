package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.entity.Company;
import com.busgallery.busgallery.entity.Region;
import com.busgallery.busgallery.service.CompanyService;
import com.busgallery.busgallery.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;
    private final CompanyService companyService;

    @GetMapping
    public List<Region> list(@RequestParam(value = "parentId", required = false) Long parentId) {
        if (parentId == null) {
            return regionService.findAll();
        }
        return regionService.findChildren(parentId);
    }

    @GetMapping("/{id}")
    public Region detail(@PathVariable Long id) {
        return regionService.findById(id);
    }

    /**
     * 地区分类：显示该地区下所有公司
     */
    @GetMapping("/{id}/companies")
    public List<Company> listCompanies(@PathVariable Long id) {
        return companyService.findByRegion(id);
    }

    @PostMapping
    public Region create(@RequestBody Region region) {
        return regionService.create(region);
    }

    @PutMapping("/{id}")
    public Region update(@PathVariable Long id, @RequestBody Region region) {
        region.setId(id);
        return regionService.update(region);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        regionService.delete(id);
    }
}