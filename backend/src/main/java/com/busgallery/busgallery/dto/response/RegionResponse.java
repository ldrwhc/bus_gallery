package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.entity.Region;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RegionResponse类用于封装RegionResponse相关的领域职责（所在包：com.busgallery.busgallery.dto.response）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionResponse {
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;

    /**
     * fromEntity方法用于处理fromEntity相关的业务逻辑。
     * @param region region参数，详见调用方上下文。
     * @return 返回RegionResponse类型结果。
     */
    public static RegionResponse fromEntity(Region region) {
        if (region == null) {
            return null;
        }
        return new RegionResponse(region.getId(), region.getName(), region.getParentId(), region.getLevel());
    }
}