package com.busgallery.busgallery.dto.request;

import com.busgallery.busgallery.entity.Brand;
import lombok.Data;

@Data
public class BrandRequest {

    private String name;
    private String country;
    private String description;

    public Brand toEntity() {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setCountry(country);
        brand.setDescription(description);
        return brand;
    }
}