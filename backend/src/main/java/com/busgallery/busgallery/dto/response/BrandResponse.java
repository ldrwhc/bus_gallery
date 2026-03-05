package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.entity.Brand;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponse {

    private Long id;
    private String name;
    private String country;
    private String description;

    public static BrandResponse fromEntity(Brand brand) {
        if (brand == null) {
            return null;
        }
        return new BrandResponse(brand.getId(), brand.getName(), brand.getCountry(), brand.getDescription());
    }
}