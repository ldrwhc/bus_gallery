package com.busgallery.busgallery.dto.request;

import com.busgallery.busgallery.entity.Brand;
import com.busgallery.busgallery.entity.Model;
import lombok.Data;

@Data
public class ModelRequest {

    private String name;
    private String description;
    private Long brandId;

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