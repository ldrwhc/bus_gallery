package com.busgallery.busgallery.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BusEventProperties.class)
public class RabbitEventConfig {

    public static final String COMMENT_CREATED_QUEUE = "busgallery.comment.created.queue";
    public static final String FAVORITE_TOGGLED_QUEUE = "busgallery.favorite.toggled.queue";

    public static final String COMMENT_CREATED_DLQ = "busgallery.comment.created.dlq";
    public static final String FAVORITE_TOGGLED_DLQ = "busgallery.favorite.toggled.dlq";

    public static final String COMMENT_CREATED_DLQ_ROUTING_KEY = "comment.created.dlq";
    public static final String FAVORITE_TOGGLED_DLQ_ROUTING_KEY = "favorite.toggled.dlq";

    @Bean
    public TopicExchange busEventExchange(BusEventProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    public DirectExchange busEventDlxExchange(BusEventProperties properties) {
        return new DirectExchange(properties.getDlxExchange(), true, false);
    }

    @Bean
    public Queue commentCreatedQueue(BusEventProperties properties) {
        return QueueBuilder.durable(COMMENT_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", properties.getDlxExchange())
                .withArgument("x-dead-letter-routing-key", COMMENT_CREATED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue favoriteToggledQueue(BusEventProperties properties) {
        return QueueBuilder.durable(FAVORITE_TOGGLED_QUEUE)
                .withArgument("x-dead-letter-exchange", properties.getDlxExchange())
                .withArgument("x-dead-letter-routing-key", FAVORITE_TOGGLED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue commentCreatedDlq() {
        return QueueBuilder.durable(COMMENT_CREATED_DLQ).build();
    }

    @Bean
    public Queue favoriteToggledDlq() {
        return QueueBuilder.durable(FAVORITE_TOGGLED_DLQ).build();
    }

    @Bean
    public Binding commentCreatedBinding(@Qualifier("commentCreatedQueue") Queue commentCreatedQueue,
                                         TopicExchange busEventExchange,
                                         BusEventProperties properties) {
        return BindingBuilder.bind(commentCreatedQueue)
                .to(busEventExchange)
                .with(properties.getCommentCreatedRoutingKey());
    }

    @Bean
    public Binding favoriteToggledBinding(@Qualifier("favoriteToggledQueue") Queue favoriteToggledQueue,
                                          TopicExchange busEventExchange,
                                          BusEventProperties properties) {
        return BindingBuilder.bind(favoriteToggledQueue)
                .to(busEventExchange)
                .with(properties.getFavoriteToggledRoutingKey());
    }

    @Bean
    public Binding commentCreatedDlqBinding(@Qualifier("commentCreatedDlq") Queue commentCreatedDlq,
                                            DirectExchange busEventDlxExchange) {
        return BindingBuilder.bind(commentCreatedDlq)
                .to(busEventDlxExchange)
                .with(COMMENT_CREATED_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding favoriteToggledDlqBinding(@Qualifier("favoriteToggledDlq") Queue favoriteToggledDlq,
                                             DirectExchange busEventDlxExchange) {
        return BindingBuilder.bind(favoriteToggledDlq)
                .to(busEventDlxExchange)
                .with(FAVORITE_TOGGLED_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        rabbitTemplate.setBeforePublishPostProcessors(persistentMessageProcessor());
        return rabbitTemplate;
    }

    private MessagePostProcessor persistentMessageProcessor() {
        return message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        };
    }
}
