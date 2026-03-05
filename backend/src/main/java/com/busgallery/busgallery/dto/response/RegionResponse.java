package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.entity.Region;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionResponse {
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;

    public static RegionResponse fromEntity(Region region) {
        if (region == null) {
            return null;
        }
        return new RegionResponse(region.getId(), region.getName(), region.getParentId(), region.getLevel());
    }
}