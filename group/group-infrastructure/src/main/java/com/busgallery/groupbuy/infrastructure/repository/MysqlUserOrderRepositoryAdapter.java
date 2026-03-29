package com.busgallery.groupbuy.infrastructure.repository;

import com.busgallery.groupbuy.domain.model.UserOrderAggregate;
import com.busgallery.groupbuy.domain.port.UserOrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * MySQL adapter for user order repository.
 */
@Repository
@RequiredArgsConstructor
public class MysqlUserOrderRepositoryAdapter implements UserOrderRepositoryPort {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public UserOrderAggregate findByOutTradeNo(String outTradeNo) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT order_id, team_id, activity_id, goods_id, user_id, out_trade_no, source, channel, order_mode, " +
                            "original_price, deduction_price, pay_price, status, out_trade_time, created_at " +
                            "FROM trade_order_item WHERE out_trade_no = ? LIMIT 1",
                    (rs, rowNum) -> UserOrderAggregate.builder()
                            .orderId(rs.getString("order_id"))
                            .teamId(rs.getString("team_id"))
                            .activityId(rs.getLong("activity_id"))
                            .goodsId(rs.getString("goods_id"))
                            .userId(rs.getLong("user_id"))
                            .outTradeNo(rs.getString("out_trade_no"))
                            .source(rs.getString("source"))
                            .channel(rs.getString("channel"))
                            .orderMode(rs.getInt("order_mode"))
                            .originalPriceCents(rs.getLong("original_price"))
                            .deductionPriceCents(rs.getLong("deduction_price"))
                            .payPriceCents(rs.getLong("pay_price"))
                            .status(TradeStatusMapper.toOrderStatus(rs.getInt("status")))
                            .outTradeTime(toLocalDateTime(rs.getTimestamp("out_trade_time")))
                            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                            .build(),
                    outTradeNo
            );
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    @Override
    public void save(UserOrderAggregate aggregate) {
        Timestamp outTradeTime = toTimestamp(aggregate.getOutTradeTime());
        int statusValue = TradeStatusMapper.toOrderStatusValue(aggregate.getStatus());
        Timestamp paySuccessTime = "PAID".equals(aggregate.getStatus()) ? outTradeTime : null;
        Timestamp refundTime = "REFUNDED".equals(aggregate.getStatus()) ? Timestamp.valueOf(LocalDateTime.now()) : null;

        int updated = jdbcTemplate.update(
                "UPDATE trade_order_item SET status = ?, out_trade_time = ?, pay_success_time = COALESCE(?, pay_success_time), " +
                        "refund_time = COALESCE(?, refund_time), updated_at = NOW() WHERE out_trade_no = ?",
                statusValue,
                outTradeTime,
                paySuccessTime,
                refundTime,
                aggregate.getOutTradeNo()
        );
        if (updated > 0) {
            return;
        }

        TradeUserSnapshot snapshot = resolveTradeUserSnapshot(aggregate.getUserId());

        jdbcTemplate.update(
                "INSERT INTO trade_order_item " +
                        "(order_id, team_id, activity_id, goods_id, user_id, trade_user_id, username_snapshot, display_name_snapshot, " +
                        "order_mode, source, channel, original_price, deduction_price, pay_price, status, out_trade_no, out_trade_time, " +
                        "pay_success_time, refund_time, biz_id, idempotency_key, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                aggregate.getOrderId(),
                aggregate.getTeamId(),
                aggregate.getActivityId(),
                aggregate.getGoodsId(),
                aggregate.getUserId(),
                snapshot.tradeUserId(),
                snapshot.usernameSnapshot(),
                snapshot.displayNameSnapshot(),
                aggregate.getOrderMode() == 2 ? 2 : 1,
                aggregate.getSource(),
                aggregate.getChannel(),
                aggregate.getOriginalPriceCents(),
                aggregate.getDeductionPriceCents(),
                aggregate.getPayPriceCents(),
                statusValue,
                aggregate.getOutTradeNo(),
                outTradeTime,
                paySuccessTime,
                refundTime,
                aggregate.getOrderId() + ":" + aggregate.getOutTradeNo(),
                aggregate.getOutTradeNo()
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private TradeUserSnapshot resolveTradeUserSnapshot(Long appUserId) {
        if (appUserId == null) {
            return TradeUserSnapshot.empty();
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT trade_user_id, username_snapshot, display_name_snapshot FROM trade_user_map WHERE app_user_id = ? LIMIT 1",
                    (rs, rowNum) -> new TradeUserSnapshot(
                            rs.getString("trade_user_id"),
                            rs.getString("username_snapshot"),
                            rs.getString("display_name_snapshot")
                    ),
                    appUserId
            );
        } catch (EmptyResultDataAccessException ignored) {
            return TradeUserSnapshot.empty();
        }
    }

    private record TradeUserSnapshot(String tradeUserId, String usernameSnapshot, String displayNameSnapshot) {
        private static TradeUserSnapshot empty() {
            return new TradeUserSnapshot(null, null, null);
        }
    }
}
