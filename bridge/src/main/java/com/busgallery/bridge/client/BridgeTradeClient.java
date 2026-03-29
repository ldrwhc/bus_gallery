package com.busgallery.bridge.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for group trade endpoints.
 */
@FeignClient(
        name = "${bridge.trade-service-name}",
        contextId = "bridgeTradeClient"
)
public interface BridgeTradeClient {

    @PostMapping(value = "/api/v1/group/index/config", consumes = MediaType.APPLICATION_JSON_VALUE)
    String queryMarketConfig(@RequestBody(required = false) String body);

    @PostMapping(value = "/api/v1/group/trade/lock", consumes = MediaType.APPLICATION_JSON_VALUE)
    String lockOrder(@RequestBody(required = false) String body);

    @PostMapping(value = "/api/v1/group/trade/settle", consumes = MediaType.APPLICATION_JSON_VALUE)
    String settleOrder(@RequestBody(required = false) String body);

    @PostMapping(value = "/api/v1/group/trade/refund", consumes = MediaType.APPLICATION_JSON_VALUE)
    String refundOrder(@RequestBody(required = false) String body);

    @GetMapping("/api/v1/group/portal/teams")
    String listTeams(@RequestParam("activityId") Long activityId,
                     @RequestParam(name = "limit", defaultValue = "5") Integer limit);

    @PostMapping(value = "/api/v1/group/portal/direct-buy", consumes = MediaType.APPLICATION_JSON_VALUE)
    String directBuy(@RequestBody(required = false) String body);

    @PostMapping(value = "/api/v1/group/portal/group-buy", consumes = MediaType.APPLICATION_JSON_VALUE)
    String groupBuy(@RequestBody(required = false) String body);

    @GetMapping("/api/v1/group/portal/messages")
    String listMessages(@RequestParam(name = "limit", defaultValue = "20") Integer limit);

    @GetMapping("/api/v1/group/portal/records")
    String listRecords(@RequestParam(name = "limit", defaultValue = "50") Integer limit);
}
