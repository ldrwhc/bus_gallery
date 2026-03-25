package com.busgallery.busgallery.messaging;

import com.busgallery.busgallery.messaging.payload.CommentCreatedEvent;
import com.busgallery.busgallery.messaging.payload.FavoriteToggledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final BusEventProperties properties;

    public void publishCommentCreated(CommentCreatedEvent event) {
        publishAfterCommit(properties.getCommentCreatedRoutingKey(), event);
    }

    public void publishFavoriteToggled(FavoriteToggledEvent event) {
        publishAfterCommit(properties.getFavoriteToggledRoutingKey(), event);
    }

    private void publishAfterCommit(String routingKey, Object payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishNow(routingKey, payload);
                }
            });
            return;
        }
        publishNow(routingKey, payload);
    }

    private void publishNow(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(properties.getExchange(), routingKey, payload);
        } catch (Exception e) {
            log.error("Publish domain event failed. routingKey={}", routingKey, e);
        }
    }
}

