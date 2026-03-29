package com.busgallery.busgallery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Content image to trade goods/activity binding response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeBindingResponse {
    private Long imageId;
    private Long vehicleId;
    private String goodsId;
    private Long activityId;
    private String goodsTitle;
    private String coverUrl;
    private Long originalPriceCents;
    private Long groupPriceCents;
    private Integer targetCount;
    private Integer validMinutes;
    private Integer activeTeamCount;
}
