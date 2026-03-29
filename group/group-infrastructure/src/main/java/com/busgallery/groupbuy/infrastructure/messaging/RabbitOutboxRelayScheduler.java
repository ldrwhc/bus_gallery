package com.busgallery.groupbuy.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Poll MySQL outbox and relay pending events to RabbitMQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitOutboxRelayScheduler {
    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${group.trade.mq.exchange:group.trade.exchange}")
    private String exchange;

    @Value("${group.trade.mq.order-locked-routing-key:trade.order.locked}")
    private String orderLockedRoutingKey;

    @Value("${group.trade.mq.order-settled-routing-key:trade.order.settled}")
    private String orderSettledRoutingKey;

    @Value("${group.trade.mq.order-refunded-routing-key:trade.order.refunded}")
    private String orderRefundedRoutingKey;

    @Value("${group.trade.outbox.relay-batch-size:100}")
    private int relayBatchSize;

    @Value("${group.trade.outbox.retry-delay-seconds:60}")
    private int retryDelaySeconds;

    @Scheduled(fixedDelayString = "${group.trade.outbox.relay-delay-ms:2000}")
    public void relayPendingEvents() {
        List<OutboxEventRow> events = jdbcTemplate.query(
                "SELECT id, event_type, payload_json FROM trade_outbox_event " +
                        "WHERE publish_status IN (0, 2) AND (next_retry_at IS NULL OR next_retry_at <= NOW()) " +
                        "ORDER BY id ASC LIMIT ?",
                (rs, rowNum) -> mapRow(rs),
                Math.max(1, relayBatchSize)
        );
        if (events.isEmpty()) {
            return;
        }
        for (OutboxEventRow event : events) {
            relayOne(event);
        }
    }

    private void relayOne(OutboxEventRow event) {
        int claimed = jdbcTemplate.update(
                "UPDATE trade_outbox_event SET next_retry_at = DATE_ADD(NOW(), INTERVAL 30 SECOND), updated_at = NOW() " +
                        "WHERE id = ? AND publish_status IN (0, 2) AND (next_retry_at IS NULL OR next_retry_at <= NOW())",
                event.id()
        );
        if (claimed <= 0) {
            return;
        }

        String routingKey = resolveRoutingKey(event.eventType());
        if (routingKey == null) {
            markFailed(event.id(), "unknown eventType: " + event.eventType());
            return;
        }
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event.payloadJson());
            jdbcTemplate.update(
                    "UPDATE trade_outbox_event SET publish_status = 1, published_at = NOW(), next_retry_at = NULL, updated_at = NOW() " +
                            "WHERE id = ?",
                    event.id()
            );
        } catch (Exception ex) {
            markFailed(event.id(), ex.getMessage());
            log.warn("outbox relay failed, id={}, eventType={}, reason={}", event.id(), event.eventType(), ex.getMessage());
        }
    }

    private void markFailed(Long id, String errorMessage) {
        jdbcTemplate.update(
                "UPDATE trade_outbox_event SET publish_status = 2, retry_count = retry_count + 1, " +
                        "next_retry_at = DATE_ADD(NOW(), INTERVAL ? SECOND), updated_at = NOW() WHERE id = ?",
                Math.max(1, retryDelaySeconds),
                id
        );
    }

    private String resolveRoutingKey(String eventType) {
        if ("ORDER_LOCKED".equals(eventType)) {
            return orderLockedRoutingKey;
        }
        if ("ORDER_SETTLED".equals(eventType)) {
            return orderSettledRoutingKey;
        }
        if ("ORDER_REFUNDED".equals(eventType)) {
            return orderRefundedRoutingKey;
        }
        return null;
    }

    private OutboxEventRow mapRow(ResultSet rs) throws SQLException {
        return new OutboxEventRow(
                rs.getLong("id"),
                rs.getString("event_type"),
                rs.getString("payload_json"),
                LocalDateTime.now()
        );
    }

    private record OutboxEventRow(Long id, String eventType, String payloadJson, LocalDateTime fetchedAt) {
    }
}
