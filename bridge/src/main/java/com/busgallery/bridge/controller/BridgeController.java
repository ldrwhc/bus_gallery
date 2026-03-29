package com.busgallery.bridge.controller;

import com.busgallery.bridge.auth.RequireLogin;
import com.busgallery.bridge.client.BridgeContentClient;
import com.busgallery.bridge.client.BridgeTradeClient;
import com.busgallery.bridge.config.BridgeProperties;
import com.busgallery.bridge.service.BridgeProxyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bridge APIs exposed to frontend.
 * This controller only orchestrates fixed business endpoints.
 */
@RestController
@RequestMapping("/api/bridge")
@RequireLogin
@RequiredArgsConstructor
public class BridgeController {

    private final BridgeTradeClient bridgeTradeClient;
    private final BridgeContentClient bridgeContentClient;
    private final BridgeProxyService bridgeProxyService;
    private final BridgeProperties bridgeProperties;

    /**
     * Resolve or create one image binding.
     */
    @PostMapping("/images/{imageId}/binding")
    public ResponseEntity<String> resolveBinding(@PathVariable("imageId") Long imageId,
                                                 @RequestBody(required = false) String body) {
        return jsonOk(bridgeContentClient.resolveBinding(imageId, body));
    }

    /**
     * Query existing goods binding.
     */
    @GetMapping("/goods/{goodsId}/binding")
    public ResponseEntity<String> queryGoodsBinding(@PathVariable("goodsId") String goodsId) {
        return jsonOk(bridgeContentClient.queryGoodsBinding(goodsId));
    }

    /**
     * Query market config.
     */
    @PostMapping("/index/config")
    public ResponseEntity<String> queryMarketConfig(@RequestBody(required = false) String body) {
        return jsonOk(bridgeTradeClient.queryMarketConfig(body));
    }

    /**
     * Lock order.
     */
    @PostMapping("/trade/lock")
    public ResponseEntity<String> lockOrder(@RequestBody(required = false) String body) {
        return jsonOk(bridgeTradeClient.lockOrder(body));
    }

    /**
     * Settle order.
     */
    @PostMapping("/trade/settle")
    public ResponseEntity<String> settleOrder(@RequestBody(required = false) String body) {
        return jsonOk(bridgeTradeClient.settleOrder(body));
    }

    /**
     * Refund order.
     */
    @PostMapping("/trade/refund")
    public ResponseEntity<String> refundOrder(@RequestBody(required = false) String body) {
        return jsonOk(bridgeTradeClient.refundOrder(body));
    }

    /**
     * List active teams.
     */
    @GetMapping("/portal/teams")
    public ResponseEntity<String> listTeams(@RequestParam("activityId") Long activityId,
                                            @RequestParam(name = "limit", defaultValue = "5") Integer limit) {
        return jsonOk(bridgeTradeClient.listTeams(activityId, limit));
    }

    /**
     * Direct buy.
     */
    @PostMapping("/portal/direct-buy")
    public ResponseEntity<String> directBuy(@RequestBody(required = false) String body) {
        return jsonOk(bridgeTradeClient.directBuy(body));
    }

    /**
     * Group buy.
     */
    @PostMapping("/portal/group-buy")
    public ResponseEntity<String> groupBuy(@RequestBody(required = false) String body) {
        return jsonOk(bridgeTradeClient.groupBuy(body));
    }

    /**
     * List user messages.
     */
    @GetMapping("/portal/messages")
    public ResponseEntity<String> listMessages(@RequestParam(name = "limit", defaultValue = "20") Integer limit) {
        return jsonOk(bridgeTradeClient.listMessages(limit));
    }

    /**
     * List user records.
     */
    @GetMapping("/portal/records")
    public ResponseEntity<String> listRecords(@RequestParam(name = "limit", defaultValue = "50") Integer limit) {
        String response = bridgeTradeClient.listRecords(limit);
        String rewritten = bridgeProxyService.rewriteRecordDownloadUrls(response);
        return jsonOk(rewritten);
    }

    /**
     * Download purchased original image.
     */
    @GetMapping("/purchases/{recordId}/download")
    public ResponseEntity<byte[]> downloadPurchasedOriginal(@PathVariable("recordId") String recordId,
                                                            HttpServletRequest request) {
        String target = bridgeProperties.getContentServiceBaseUrl()
                + "/api/trade-bridge/purchases/" + recordId + "/download";
        return bridgeProxyService.proxyBinaryGet(target, request);
    }

    private ResponseEntity<String> jsonOk(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }
}
