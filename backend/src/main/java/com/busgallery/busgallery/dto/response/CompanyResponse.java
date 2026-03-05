package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.entity.Company;
import com.busgallery.busgallery.entity.Region;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {

    private Long id;
    private String name;
    private String description;
    private RegionSimple region;

    public static CompanyResponse fromEntity(Company company) {
        if (company == null) {
            return null;
        }
        Region regionEntity = company.getRegion();
        RegionSimple region = regionEntity == null ? null :
                new RegionSimple(regionEntity.getId(), regionEntity.getName());
        return new CompanyResponse(company.getId(), company.getName(), company.getDescription(), region);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionSimple {
        private Long id;
        private String name;
    }
}