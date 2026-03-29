package com.busgallery.groupbuy.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for trade events.
 */
@Configuration
public class TradeRabbitTopologyConfig {

    @Bean
    public TopicExchange groupTradeExchange(@Value("${group.trade.mq.exchange:group.trade.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue orderLockedQueue() {
        return QueueBuilder.durable("group.trade.order.locked.queue").build();
    }

    @Bean
    public Queue orderSettledQueue() {
        return QueueBuilder.durable("group.trade.order.settled.queue").build();
    }

    @Bean
    public Queue orderRefundedQueue() {
        return QueueBuilder.durable("group.trade.order.refunded.queue").build();
    }

    @Bean
    public Binding orderLockedBinding(TopicExchange groupTradeExchange,
                                      Queue orderLockedQueue,
                                      @Value("${group.trade.mq.order-locked-routing-key:trade.order.locked}") String routingKey) {
        return BindingBuilder.bind(orderLockedQueue).to(groupTradeExchange).with(routingKey);
    }

    @Bean
    public Binding orderSettledBinding(TopicExchange groupTradeExchange,
                                       Queue orderSettledQueue,
                                       @Value("${group.trade.mq.order-settled-routing-key:trade.order.settled}") String routingKey) {
        return BindingBuilder.bind(orderSettledQueue).to(groupTradeExchange).with(routingKey);
    }

    @Bean
    public Binding orderRefundedBinding(TopicExchange groupTradeExchange,
                                        Queue orderRefundedQueue,
                                        @Value("${group.trade.mq.order-refunded-routing-key:trade.order.refunded}") String routingKey) {
        return BindingBuilder.bind(orderRefundedQueue).to(groupTradeExchange).with(routingKey);
    }
}
