package com.busgallery.groupbuy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request for portal checkout actions.
 */
@Data
public class PortalCheckoutRequest {
    @NotBlank(message = "goodsId is required")
    private String goodsId;

    @NotNull(message = "activityId is required")
    private Long activityId;

    /**
     * Optional existing team id for joining a running team.
     */
    private String teamId;

    @NotBlank(message = "source is required")
    private String source = "content";

    @NotBlank(message = "channel is required")
    private String channel = "web";
}

