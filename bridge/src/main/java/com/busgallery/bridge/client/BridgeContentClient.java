package com.busgallery.bridge.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for content domain bridge endpoints.
 */
@FeignClient(
        name = "${bridge.content-service-name}",
        url = "${bridge.content-service-url:}",
        contextId = "bridgeContentClient"
)
public interface BridgeContentClient {

    /**
     * Resolve/create trade binding for image.
     *
     * @param imageId image id
     * @param body request body
     * @return downstream json body
     */
    @PostMapping(value = "/api/trade-bridge/images/{imageId}/binding", consumes = MediaType.APPLICATION_JSON_VALUE)
    String resolveBinding(@PathVariable("imageId") Long imageId,
                          @RequestBody(required = false) String body);

    /**
     * Query existing goods binding by goods id.
     *
     * @param goodsId goods id
     * @return downstream json body
     */
    @GetMapping("/api/trade-bridge/goods/{goodsId}/binding")
    String queryGoodsBinding(@PathVariable("goodsId") String goodsId);
}
