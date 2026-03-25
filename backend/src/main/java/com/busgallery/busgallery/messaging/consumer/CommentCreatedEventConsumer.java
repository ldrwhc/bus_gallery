package com.busgallery.busgallery.messaging.consumer;

import com.busgallery.busgallery.messaging.RabbitEventConfig;
import com.busgallery.busgallery.messaging.payload.CommentCreatedEvent;
import com.busgallery.busgallery.messaging.sideeffect.CommentSideEffectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentCreatedEventConsumer {

    private final CommentSideEffectService commentSideEffectService;

    @RabbitListener(queues = RabbitEventConfig.COMMENT_CREATED_QUEUE)
    public void onCommentCreated(CommentCreatedEvent event) {
        try {
            commentSideEffectService.process(event);
        } catch (Exception e) {
            log.error("consume comment.created failed", e);
        }
    }
}
