package com.busgallery.busgallery.dto.request;

import com.busgallery.busgallery.entity.Brand;
import com.busgallery.busgallery.entity.Model;
import lombok.Data;

/**
 * ModelRequest类用于封装ModelRequest相关的领域职责（所在包：com.busgallery.busgallery.dto.request）。
 */
@Data
public class ModelRequest {

    private String name;
    private String description;
    private Long brandId;

    /**
     * toEntity方法用于处理toEntity相关的业务逻辑。
     * @return 返回Model类型结果。
     */
    public Model toEntity() {
        Model model = new Model();
        model.setName(name);
        model.setDescription(description);
        if (brandId != null) {
            Brand brand = new Brand();
            brand.setId(brandId);
            model.setBrand(brand);
        }
        return model;
    }
}