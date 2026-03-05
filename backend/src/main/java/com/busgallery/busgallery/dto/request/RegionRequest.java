package com.busgallery.busgallery.dto.request;

import com.busgallery.busgallery.entity.Region;
import lombok.Data;

@Data
public class RegionRequest {

    private String name;
    private Long parentId;
    private Integer level;

    public Region toEntity() {
        Region region = new Region();
        region.setName(name);
        region.setParentId(parentId);
        region.setLevel(level);
        return region;
    }
}