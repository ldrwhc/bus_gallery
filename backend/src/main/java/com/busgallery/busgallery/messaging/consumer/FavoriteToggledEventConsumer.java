package com.busgallery.busgallery.messaging.consumer;

import com.busgallery.busgallery.messaging.RabbitEventConfig;
import com.busgallery.busgallery.messaging.payload.FavoriteToggledEvent;
import com.busgallery.busgallery.messaging.sideeffect.FavoriteSideEffectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FavoriteToggledEventConsumer {

    private final FavoriteSideEffectService favoriteSideEffectService;

    @RabbitListener(queues = RabbitEventConfig.FAVORITE_TOGGLED_QUEUE)
    public void onFavoriteToggled(FavoriteToggledEvent event) {
        try {
            favoriteSideEffectService.process(event);
        } catch (Exception e) {
            log.error("consume favorite.toggled failed", e);
        }
    }
}
