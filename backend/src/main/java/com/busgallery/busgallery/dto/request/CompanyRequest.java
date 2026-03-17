package com.busgallery.busgallery.dto.request;

import com.busgallery.busgallery.entity.Company;
import com.busgallery.busgallery.entity.Region;
import lombok.Data;

/**
 * CompanyRequest类用于封装CompanyRequest相关的领域职责（所在包：com.busgallery.busgallery.dto.request）。
 */
@Data
public class CompanyRequest {

    private String name;
    private String description;
    private Long regionId;

    /**
     * toEntity方法用于处理toEntity相关的业务逻辑。
     * @return 返回Company类型结果。
     */
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