package com.busgallery.groupbuy.infrastructure.repository;

import com.busgallery.groupbuy.domain.model.TeamOrderAggregate;
import com.busgallery.groupbuy.domain.port.TeamOrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * MySQL adapter for team order repository.
 */
@Repository
@RequiredArgsConstructor
public class MysqlTeamOrderRepositoryAdapter implements TeamOrderRepositoryPort {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public TeamOrderAggregate findByTeamId(String teamId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT team_id, activity_id, goods_id, owner_user_id, original_price, deduction_price, pay_price, " +
                            "target_count, lock_count, complete_count, status, valid_start_time, valid_end_time " +
                            "FROM trade_order_team WHERE team_id = ? LIMIT 1",
                    (rs, rowNum) -> TeamOrderAggregate.builder()
                            .teamId(rs.getString("team_id"))
                            .activityId(rs.getLong("activity_id"))
                            .goodsId(rs.getString("goods_id"))
                            .ownerUserId(rs.getLong("owner_user_id"))
                            .originalPriceCents(rs.getLong("original_price"))
                            .deductionPriceCents(rs.getLong("deduction_price"))
                            .payPriceCents(rs.getLong("pay_price"))
                            .targetCount(rs.getInt("target_count"))
                            .lockCount(rs.getInt("lock_count"))
                            .completeCount(rs.getInt("complete_count"))
                            .status(TradeStatusMapper.toTeamStatus(rs.getInt("status")))
                            .validStartTime(toLocalDateTime(rs.getTimestamp("valid_start_time")))
                            .validEndTime(toLocalDateTime(rs.getTimestamp("valid_end_time")))
                            .build(),
                    teamId
            );
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    @Override
    public void save(TeamOrderAggregate aggregate) {
        int updated = jdbcTemplate.update(
                "UPDATE trade_order_team SET lock_count = ?, complete_count = ?, status = ?, " +
                        "valid_start_time = ?, valid_end_time = ?, updated_at = NOW() WHERE team_id = ?",
                aggregate.getLockCount(),
                aggregate.getCompleteCount(),
                TradeStatusMapper.toTeamStatusValue(aggregate.getStatus()),
                toTimestamp(aggregate.getValidStartTime()),
                toTimestamp(aggregate.getValidEndTime()),
                aggregate.getTeamId()
        );
        if (updated > 0) {
            return;
        }

        String ownerTradeUserId = resolveTradeUserId(aggregate.getOwnerUserId());

        jdbcTemplate.update(
                "INSERT INTO trade_order_team " +
                        "(team_id, activity_id, goods_id, owner_user_id, original_price, deduction_price, pay_price, " +
                        "target_count, lock_count, complete_count, status, valid_start_time, valid_end_time, owner_trade_user_id, notify_type, notify_url, version, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'MQ', NULL, 0, NOW(), NOW())",
                aggregate.getTeamId(),
                aggregate.getActivityId(),
                aggregate.getGoodsId(),
                aggregate.getOwnerUserId(),
                aggregate.getOriginalPriceCents(),
                aggregate.getDeductionPriceCents(),
                aggregate.getPayPriceCents(),
                aggregate.getTargetCount(),
                aggregate.getLockCount(),
                aggregate.getCompleteCount(),
                TradeStatusMapper.toTeamStatusValue(aggregate.getStatus()),
                toTimestamp(aggregate.getValidStartTime()),
                toTimestamp(aggregate.getValidEndTime()),
                ownerTradeUserId
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private String resolveTradeUserId(Long appUserId) {
        if (appUserId == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT trade_user_id FROM trade_user_map WHERE app_user_id = ? LIMIT 1",
                    String.class,
                    appUserId
            );
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }
}
