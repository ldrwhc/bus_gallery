package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.entity.Model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ModelResponse类用于封装ModelResponse相关的领域职责（所在包：com.busgallery.busgallery.dto.response）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelResponse {

    private Long id;
    private String name;
    private String description;
    private Long brandId;
    private String brandName;

    /**
     * fromEntity方法用于处理fromEntity相关的业务逻辑。
     * @param model model参数，详见调用方上下文。
     * @return 返回ModelResponse类型结果。
     */
    public static ModelResponse fromEntity(Model model) {
        if (model == null) {
            return null;
        }
        Long brandId = model.getBrand() != null ? model.getBrand().getId() : null;
        String brandName = model.getBrand() != null ? model.getBrand().getName() : null;
        return new ModelResponse(model.getId(), model.getName(), model.getDescription(), brandId, brandName);
    }
}