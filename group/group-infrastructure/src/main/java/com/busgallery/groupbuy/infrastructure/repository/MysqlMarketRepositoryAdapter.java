package com.busgallery.groupbuy.infrastructure.repository;

import com.busgallery.groupbuy.domain.model.ActivityAggregate;
import com.busgallery.groupbuy.domain.model.GoodsAggregate;
import com.busgallery.groupbuy.domain.port.ActivityRepositoryPort;
import com.busgallery.groupbuy.domain.port.GoodsRepositoryPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * MySQL + Redis adapter for market repositories.
 */
@Repository
@RequiredArgsConstructor
public class MysqlMarketRepositoryAdapter implements GoodsRepositoryPort, ActivityRepositoryPort {
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final String GOODS_KEY_PREFIX = "group:goods:";
    private static final String ACTIVITY_KEY_PREFIX = "group:activity:";

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public GoodsAggregate findByGoodsId(String goodsId) {
        String cacheKey = GOODS_KEY_PREFIX + goodsId;
        GoodsAggregate cached = readCache(cacheKey, GoodsAggregate.class);
        if (cached != null) {
            return cached;
        }
        try {
            GoodsAggregate aggregate = jdbcTemplate.queryForObject(
                    "SELECT goods_id, title, original_price, group_price, stock_available, status " +
                            "FROM trade_goods WHERE goods_id = ? LIMIT 1",
                    (rs, rowNum) -> GoodsAggregate.builder()
                            .goodsId(rs.getString("goods_id"))
                            .title(rs.getString("title"))
                            .originalPriceCents(rs.getLong("original_price"))
                            .groupPriceCents(rs.getLong("group_price"))
                            .stockAvailable(rs.getInt("stock_available"))
                            .enabled(rs.getInt("status") == 1)
                            .build(),
                    goodsId
            );
            writeCache(cacheKey, aggregate);
            return aggregate;
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    @Override
    public ActivityAggregate findByActivityId(Long activityId) {
        String cacheKey = ACTIVITY_KEY_PREFIX + activityId;
        ActivityAggregate cached = readCache(cacheKey, ActivityAggregate.class);
        if (cached != null) {
            return cached;
        }
        try {
            ActivityAggregate aggregate = jdbcTemplate.queryForObject(
                    "SELECT activity_id, goods_id, activity_name, target_count, valid_minutes, status, start_time, end_time " +
                            "FROM trade_activity WHERE activity_id = ? LIMIT 1",
                    (rs, rowNum) -> mapActivity(rs.getLong("activity_id"),
                            rs.getString("goods_id"),
                            rs.getString("activity_name"),
                            rs.getInt("target_count"),
                            rs.getInt("valid_minutes"),
                            rs.getInt("status"),
                            rs.getTimestamp("start_time"),
                            rs.getTimestamp("end_time")),
                    activityId
            );
            writeCache(cacheKey, aggregate);
            return aggregate;
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    @Override
    public ActivityAggregate findActiveByGoodsId(String goodsId) {
        String cacheKey = ACTIVITY_KEY_PREFIX + "goods:" + goodsId;
        ActivityAggregate cached = readCache(cacheKey, ActivityAggregate.class);
        if (cached != null && cached.isEnabled()) {
            return cached;
        }

        ActivityAggregate active = findActivityByGoodsWithinWindow(goodsId);
        if (active != null && active.isEnabled()) {
            writeCache(cacheKey, active);
            return active;
        }

        ActivityAggregate latest = findLatestActivityByGoods(goodsId);
        if (latest != null) {
            ActivityAggregate repaired = ensureActivityWindow(latest);
            writeCache(cacheKey, repaired);
            writeCache(ACTIVITY_KEY_PREFIX + repaired.getActivityId(), repaired);
            return repaired;
        }

        ActivityAggregate created = createDefaultActivity(goodsId);
        if (created != null) {
            writeCache(cacheKey, created);
            writeCache(ACTIVITY_KEY_PREFIX + created.getActivityId(), created);
        }
        return created;
    }

    private ActivityAggregate findActivityByGoodsWithinWindow(String goodsId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            return jdbcTemplate.queryForObject(
                    "SELECT activity_id, goods_id, activity_name, target_count, valid_minutes, status, start_time, end_time " +
                            "FROM trade_activity WHERE goods_id = ? AND status = 1 " +
                            "AND start_time <= ? AND end_time >= ? ORDER BY id DESC LIMIT 1",
                    (rs, rowNum) -> mapActivity(rs.getLong("activity_id"),
                            rs.getString("goods_id"),
                            rs.getString("activity_name"),
                            rs.getInt("target_count"),
                            rs.getInt("valid_minutes"),
                            rs.getInt("status"),
                            rs.getTimestamp("start_time"),
                            rs.getTimestamp("end_time")),
                    goodsId,
                    toTimestamp(now),
                    toTimestamp(now)
            );
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private ActivityAggregate findLatestActivityByGoods(String goodsId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT activity_id, goods_id, activity_name, target_count, valid_minutes, status, start_time, end_time " +
                            "FROM trade_activity WHERE goods_id = ? ORDER BY id DESC LIMIT 1",
                    (rs, rowNum) -> mapActivity(rs.getLong("activity_id"),
                            rs.getString("goods_id"),
                            rs.getString("activity_name"),
                            rs.getInt("target_count"),
                            rs.getInt("valid_minutes"),
                            rs.getInt("status"),
                            rs.getTimestamp("start_time"),
                            rs.getTimestamp("end_time")),
                    goodsId
            );
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private ActivityAggregate ensureActivityWindow(ActivityAggregate current) {
        LocalDateTime now = LocalDateTime.now();
        boolean shouldRepair = current == null
                || !current.isEnabled()
                || current.getStartTime() == null
                || current.getEndTime() == null
                || now.isBefore(current.getStartTime())
                || now.isAfter(current.getEndTime());
        if (!shouldRepair) {
            return current;
        }

        LocalDateTime newStart = now.minusMinutes(1);
        LocalDateTime newEnd = now.plusDays(365);
        int safeTarget = current.getTargetCount() > 0 ? current.getTargetCount() : 3;
        int safeValid = current.getValidMinutes() > 0 ? current.getValidMinutes() : 15;
        String safeName = StringUtils.hasText(current.getActivityName())
                ? current.getActivityName()
                : "Auto Activity " + current.getGoodsId();

        jdbcTemplate.update(
                "UPDATE trade_activity SET status = 1, activity_name = ?, target_count = ?, valid_minutes = ?, " +
                        "start_time = ?, end_time = ?, updated_at = NOW() WHERE activity_id = ?",
                safeName,
                safeTarget,
                safeValid,
                toTimestamp(newStart),
                toTimestamp(newEnd),
                current.getActivityId()
        );

        return ActivityAggregate.builder()
                .activityId(current.getActivityId())
                .goodsId(current.getGoodsId())
                .activityName(safeName)
                .targetCount(safeTarget)
                .validMinutes(safeValid)
                .enabled(true)
                .startTime(newStart)
                .endTime(newEnd)
                .build();
    }

    private ActivityAggregate createDefaultActivity(String goodsId) {
        if (!StringUtils.hasText(goodsId)) {
            return null;
        }
        Long activityId = nextActivityId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusMinutes(1);
        LocalDateTime end = now.plusDays(365);
        int inserted = jdbcTemplate.update(
                "INSERT INTO trade_activity (activity_id, goods_id, activity_name, group_type, take_limit_count, target_count, valid_minutes, status, start_time, end_time, created_by, created_at, updated_at) " +
                        "VALUES (?, ?, ?, 0, 1, 3, 15, 1, ?, ?, NULL, NOW(), NOW())",
                activityId,
                goodsId,
                "Auto Activity " + goodsId,
                toTimestamp(start),
                toTimestamp(end)
        );
        if (inserted <= 0) {
            return null;
        }
        return ActivityAggregate.builder()
                .activityId(activityId)
                .goodsId(goodsId)
                .activityName("Auto Activity " + goodsId)
                .targetCount(3)
                .validMinutes(15)
                .enabled(true)
                .startTime(start)
                .endTime(end)
                .build();
    }

    private Long nextActivityId() {
        long base = System.currentTimeMillis();
        long suffix = ThreadLocalRandom.current().nextLong(100, 999);
        return base * 1000 + suffix;
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    @Override
    public int countActiveTeams(Long activityId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM trade_order_team WHERE activity_id = ? AND status = 0",
                Integer.class,
                activityId
        );
        return count == null ? 0 : count;
    }

    private <T> T readCache(String key, Class<T> type) {
        String raw = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, type);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private void writeCache(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), CACHE_TTL);
        } catch (Exception ignored) {
            // ignore cache failure and fallback to DB on next request
        }
    }

    private ActivityAggregate mapActivity(Long activityId,
                                          String goodsId,
                                          String activityName,
                                          int targetCount,
                                          int validMinutes,
                                          int status,
                                          Timestamp startTime,
                                          Timestamp endTime) {
        LocalDateTime start = startTime == null ? null : startTime.toLocalDateTime();
        LocalDateTime end = endTime == null ? null : endTime.toLocalDateTime();
        LocalDateTime now = LocalDateTime.now();
        boolean timeWindowValid = start != null && end != null && !now.isBefore(start) && !now.isAfter(end);
        return ActivityAggregate.builder()
                .activityId(activityId)
                .goodsId(goodsId)
                .activityName(activityName)
                .targetCount(targetCount)
                .validMinutes(validMinutes)
                .enabled(status == 1 && timeWindowValid)
                .startTime(start)
                .endTime(end)
                .build();
    }
}
