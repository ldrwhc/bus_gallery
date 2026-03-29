package com.busgallery.groupbuy.infrastructure.messaging;

import com.busgallery.groupbuy.domain.model.UserOrderAggregate;
import com.busgallery.groupbuy.domain.port.TradeEventPublisherPort;
import com.busgallery.groupbuy.types.model.IdFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Store trade events in MySQL outbox table inside current transaction.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MysqlOutboxTradeEventPublisherAdapter implements TradeEventPublisherPort {
    private static final String AGGREGATE_TYPE_ORDER = "ORDER";
    private static final String EVENT_ORDER_LOCKED = "ORDER_LOCKED";
    private static final String EVENT_ORDER_SETTLED = "ORDER_SETTLED";
    private static final String EVENT_ORDER_REFUNDED = "ORDER_REFUNDED";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishOrderLocked(UserOrderAggregate order) {
        saveOutboxEvent(order, EVENT_ORDER_LOCKED);
    }

    @Override
    public void publishOrderSettled(UserOrderAggregate order) {
        saveOutboxEvent(order, EVENT_ORDER_SETTLED);
    }

    @Override
    public void publishOrderRefunded(UserOrderAggregate order) {
        saveOutboxEvent(order, EVENT_ORDER_REFUNDED);
    }

    private void saveOutboxEvent(UserOrderAggregate order, String eventType) {
        try {
            String payloadJson = objectMapper.writeValueAsString(buildPayload(order, eventType));
            jdbcTemplate.update(
                    "INSERT INTO trade_outbox_event " +
                            "(event_id, aggregate_type, aggregate_id, event_type, event_key, payload_json, headers_json, publish_status, retry_count, next_retry_at, published_at, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, NULL, 0, 0, NULL, NULL, NOW(), NOW()) " +
                            "ON DUPLICATE KEY UPDATE updated_at = NOW()",
                    IdFactory.nextEventId(),
                    AGGREGATE_TYPE_ORDER,
                    order.getOrderId(),
                    eventType,
                    order.getOutTradeNo(),
                    payloadJson
            );
        } catch (Exception ex) {
            log.error("failed to save outbox event, eventType={}, orderId={}, reason={}",
                    eventType, order.getOrderId(), ex.getMessage(), ex);
            throw new IllegalStateException("failed to save outbox event", ex);
        }
    }

    private Map<String, Object> buildPayload(UserOrderAggregate order, String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("orderId", order.getOrderId());
        payload.put("teamId", order.getTeamId());
        payload.put("activityId", order.getActivityId());
        payload.put("goodsId", order.getGoodsId());
        payload.put("userId", order.getUserId());
        payload.put("outTradeNo", order.getOutTradeNo());
        payload.put("orderStatus", order.getStatus());
        payload.put("originalPriceCents", order.getOriginalPriceCents());
        payload.put("deductionPriceCents", order.getDeductionPriceCents());
        payload.put("payPriceCents", order.getPayPriceCents());
        payload.put("outTradeTime", order.getOutTradeTime());
        payload.put("eventCreatedAt", LocalDateTime.now());
        return payload;
    }
}
