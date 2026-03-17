package com.busgallery.busgallery.dto.request;

import com.busgallery.busgallery.entity.Brand;
import lombok.Data;

/**
 * BrandRequest类用于封装BrandRequest相关的领域职责（所在包：com.busgallery.busgallery.dto.request）。
 */
@Data
public class BrandRequest {

    private String name;
    private String country;
    private String description;

    /**
     * toEntity方法用于处理toEntity相关的业务逻辑。
     * @return 返回Brand类型结果。
     */
    public Brand toEntity() {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setCountry(country);
        brand.setDescription(description);
        return brand;
    }
}