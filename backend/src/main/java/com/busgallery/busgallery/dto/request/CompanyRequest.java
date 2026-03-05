package com.busgallery.busgallery.dto.request;

import com.busgallery.busgallery.entity.Company;
import com.busgallery.busgallery.entity.Region;
import lombok.Data;

@Data
public class CompanyRequest {

    private String name;
    private String description;
    private Long regionId;

    public Company toEntity() {
        Company company = new Company();
        company.setName(name);
        company.setDescription(description);
        if (regionId != null) {
            Region region = new Region();
            region.setId(regionId);
            company.setRegion(region);
        }
        return company;
    }
}