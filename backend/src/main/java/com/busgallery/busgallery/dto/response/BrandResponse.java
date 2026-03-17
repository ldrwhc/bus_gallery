package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.entity.Brand;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BrandResponse类用于封装BrandResponse相关的领域职责（所在包：com.busgallery.busgallery.dto.response）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponse {

    private Long id;
    private String name;
    private String country;
    private String description;

    /**
     * fromEntity方法用于处理fromEntity相关的业务逻辑。
     * @param brand brand参数，详见调用方上下文。
     * @return 返回BrandResponse类型结果。
     */
    public static BrandResponse fromEntity(Brand brand) {
        if (brand == null) {
            return null;
        }
        return new BrandResponse(brand.getId(), brand.getName(), brand.getCountry(), brand.getDescription());
    }
}