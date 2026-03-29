package com.busgallery.groupbuy.trigger.http;

import com.busgallery.groupbuy.api.dto.MarketConfigRequest;
import com.busgallery.groupbuy.api.dto.MarketConfigResponse;
import com.busgallery.groupbuy.api.service.GroupMarketApi;
import com.busgallery.groupbuy.domain.model.MarketConfigProjection;
import com.busgallery.groupbuy.domain.service.MarketDomainService;
import com.busgallery.groupbuy.types.model.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trigger layer controller for market read APIs.
 */
@RestController
@RequestMapping("/api/v1/group/index")
@RequiredArgsConstructor
public class GroupMarketController implements GroupMarketApi {
    private final MarketDomainService marketDomainService;

    /**
     * Query one goods market config.
     *
     * @param request request body
     * @return market config response
     */
    @Override
    @PostMapping("/config")
    public ApiResponse<MarketConfigResponse> queryMarketConfig(@Valid @RequestBody MarketConfigRequest request) {
        MarketConfigProjection projection = marketDomainService.queryMarketConfig(request.getGoodsId());
        MarketConfigResponse response = MarketConfigResponse.builder()
                .goodsId(projection.getGoodsId())
                .activityId(projection.getActivityId())
                .goodsTitle(projection.getGoodsTitle())
                .originalPriceCents(projection.getOriginalPriceCents())
                .groupPriceCents(projection.getGroupPriceCents())
                .targetCount(projection.getTargetCount())
                .validMinutes(projection.getValidMinutes())
                .activeTeamCount(projection.getActiveTeamCount())
                .build();
        return ApiResponse.success(response);
    }
}
