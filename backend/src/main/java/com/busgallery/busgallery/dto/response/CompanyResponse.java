package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.entity.Company;
import com.busgallery.busgallery.entity.Region;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CompanyResponse类用于封装CompanyResponse相关的领域职责（所在包：com.busgallery.busgallery.dto.response）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {

    private Long id;
    private String name;
    private String description;
    private RegionSimple region;

    /**
     * fromEntity方法用于处理fromEntity相关的业务逻辑。
     * @param company company参数，详见调用方上下文。
     * @return 返回CompanyResponse类型结果。
     */
    public static CompanyResponse fromEntity(Company company) {
        if (company == null) {
            return null;
        }
        Region regionEntity = company.getRegion();
        RegionSimple region = regionEntity == null ? null :
                new RegionSimple(regionEntity.getId(), regionEntity.getName());
        return new CompanyResponse(company.getId(), company.getName(), company.getDescription(), region);
    }

    /**
     * RegionSimple类用于封装RegionSimple相关的领域职责（所在包：com.busgallery.busgallery.dto.response）。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionSimple {
        private Long id;
        private String name;
    }
}