package com.busgallery.groupbuy.trigger.http;

import com.busgallery.groupbuy.api.dto.PortalCheckoutRequest;
import com.busgallery.groupbuy.api.dto.PortalCheckoutResponse;
import com.busgallery.groupbuy.api.dto.PortalTeamSummaryResponse;
import com.busgallery.groupbuy.api.dto.PortalUserMessageResponse;
import com.busgallery.groupbuy.api.dto.PortalUserRecordResponse;
import com.busgallery.groupbuy.trigger.auth.AuthContextHolder;
import com.busgallery.groupbuy.trigger.auth.RequireLogin;
import com.busgallery.groupbuy.trigger.service.TradePortalService;
import com.busgallery.groupbuy.types.model.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * User portal APIs for direct/group purchase, records and messages.
 */
@RestController
@RequestMapping("/api/v1/group/portal")
@RequireLogin
@RequiredArgsConstructor
public class GroupPortalController {

    private final TradePortalService tradePortalService;

    /**
     * List active grouping teams for one activity.
     *
     * @param activityId activity id
     * @param limit      response size
     * @return active teams
     */
    @GetMapping("/teams")
    public ApiResponse<List<PortalTeamSummaryResponse>> teams(@RequestParam(name = "activityId", required = false) Long activityId,
                                                              @RequestParam(name = "limit", defaultValue = "5") int limit) {
        List<PortalTeamSummaryResponse> data = tradePortalService.listActiveTeams(activityId, limit);
        return ApiResponse.success(data);
    }

    /**
     * Direct buy checkout.
     *
     * @param request checkout request
     * @return checkout result
     */
    @PostMapping("/direct-buy")
    public ApiResponse<PortalCheckoutResponse> directBuy(@Valid @RequestBody PortalCheckoutRequest request) {
        Long userId = AuthContextHolder.requirePrincipal().getUserId();
        PortalCheckoutResponse data = tradePortalService.directCheckout(userId, request);
        return ApiResponse.success(data);
    }

    /**
     * Group buy checkout.
     *
     * @param request checkout request
     * @return checkout result
     */
    @PostMapping("/group-buy")
    public ApiResponse<PortalCheckoutResponse> groupBuy(@Valid @RequestBody PortalCheckoutRequest request) {
        Long userId = AuthContextHolder.requirePrincipal().getUserId();
        PortalCheckoutResponse data = tradePortalService.groupCheckout(userId, request);
        return ApiResponse.success(data);
    }

    /**
     * List current user trade messages.
     *
     * @param limit list limit
     * @return message list
     */
    @GetMapping("/messages")
    public ApiResponse<List<PortalUserMessageResponse>> messages(@RequestParam(name = "limit", defaultValue = "20") int limit) {
        Long userId = AuthContextHolder.requirePrincipal().getUserId();
        return ApiResponse.success(tradePortalService.listUserMessages(userId, limit));
    }

    /**
     * List current user trade records.
     *
     * @param limit list limit
     * @return record list
     */
    @GetMapping("/records")
    public ApiResponse<List<PortalUserRecordResponse>> records(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        Long userId = AuthContextHolder.requirePrincipal().getUserId();
        return ApiResponse.success(tradePortalService.listUserRecords(userId, limit));
    }
}
