package com.busgallery.busgallery.messaging;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "busgallery.events")
public class BusEventProperties {

    private String exchange = "busgallery.events.exchange";

    private String dlxExchange = "busgallery.events.dlx";

    private String commentCreatedRoutingKey = "comment.created";

    private String favoriteToggledRoutingKey = "favorite.toggled";
}

