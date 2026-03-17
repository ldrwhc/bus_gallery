package com.busgallery.busgallery.dto.request;

import com.busgallery.busgallery.entity.Region;
import lombok.Data;

/**
 * RegionRequest类用于封装RegionRequest相关的领域职责（所在包：com.busgallery.busgallery.dto.request）。
 */
@Data
public class RegionRequest {

    private String name;
    private Long parentId;
    private Integer level;

    /**
     * toEntity方法用于处理toEntity相关的业务逻辑。
     * @return 返回Region类型结果。
     */
    public Region toEntity() {
        Region region = new Region();
        region.setName(name);
        region.setParentId(parentId);
        region.setLevel(level);
        return region;
    }
}