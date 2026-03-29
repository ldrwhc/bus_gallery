package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.RoleGuard;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Station-admin APIs for managing group-buy parameters per image(goods).
 */
@RestController
@RequestMapping("/api/admin/group")
@RequiredArgsConstructor
public class AdminGroupConfigController {

    private static final String TRADE_DB = "trade_center";
    private static final int DEFAULT_TARGET_COUNT = 3;
    private static final int DEFAULT_VALID_MINUTES = 1440;
    private static final int DEFAULT_ACTIVITY_DAYS = 365;

    private final JdbcTemplate jdbcTemplate;

    /**
     * List group-buy parameter rows for station dashboard.
     */
    @GetMapping("/params")
    @RequireLogin
    public List<GroupParamRow> listGroupParams() {
        RoleGuard.requireStation();
        String sql =
                "SELECT g.goods_id, g.image_id, g.vehicle_id, g.title, g.original_price, g.group_price, " +
                        "g.status AS goods_status, g.stock_available, " +
                        "v.plate_number, " +
                        "a.activity_id, a.target_count, a.valid_minutes, a.status AS activity_status, a.start_time, a.end_time " +
                        "FROM " + TRADE_DB + ".trade_goods g " +
                        "LEFT JOIN vehicle v ON v.id = g.vehicle_id " +
                        "LEFT JOIN " +
                        "(SELECT t1.* FROM " + TRADE_DB + ".trade_activity t1 " +
                        "JOIN (SELECT goods_id, MAX(id) AS max_id FROM " + TRADE_DB + ".trade_activity GROUP BY goods_id) t2 " +
                        "ON t1.id = t2.max_id) a ON a.goods_id = g.goods_id " +
                        "ORDER BY g.updated_at DESC, g.id DESC";
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    long original = rs.getLong("original_price");
                    long group = rs.getLong("group_price");
                    GroupParamRow row = new GroupParamRow();
                    row.setGoodsId(rs.getString("goods_id"));
                    row.setImageId(rs.getObject("image_id", Long.class));
                    row.setVehicleId(rs.getObject("vehicle_id", Long.class));
                    String plate = rs.getString("plate_number");
                    String title = rs.getString("title");
                    row.setImageName(StringUtils.hasText(plate) ? plate : (StringUtils.hasText(title) ? title : "图片#" + row.getImageId()));
                    row.setOriginalPriceCents(original);
                    row.setGroupPriceCents(group);
                    row.setDiscountRate(calcDiscountRate(original, group));
                    row.setGoodsStatus(rs.getInt("goods_status"));
                    row.setStockAvailable(rs.getInt("stock_available"));
                    row.setActivityId(rs.getObject("activity_id", Long.class));
                    row.setTargetCount(rs.getObject("target_count", Integer.class));
                    row.setValidMinutes(rs.getObject("valid_minutes", Integer.class));
                    row.setActivityStatus(rs.getObject("activity_status", Integer.class));
                    row.setStartTime(rs.getObject("start_time", LocalDateTime.class));
                    row.setEndTime(rs.getObject("end_time", LocalDateTime.class));
                    return row;
                }
        );
    }

    /**
     * Update one goods/activity parameter set.
     */
    @PutMapping("/params/{goodsId}")
    @RequireLogin
    @Transactional
    public GroupParamRow updateGroupParam(@PathVariable String goodsId, @RequestBody GroupParamUpdateRequest request) {
        RoleGuard.requireStation();
        if (!StringUtils.hasText(goodsId)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "goodsId is required");
        }
        GroupBaseRow base = jdbcTemplate.query(
                "SELECT goods_id, image_id, vehicle_id, title, original_price, group_price, status, stock_available " +
                        "FROM " + TRADE_DB + ".trade_goods WHERE goods_id = ? LIMIT 1",
                rs -> rs.next()
                        ? new GroupBaseRow(
                        rs.getString("goods_id"),
                        rs.getObject("image_id", Long.class),
                        rs.getObject("vehicle_id", Long.class),
                        rs.getString("title"),
                        rs.getLong("original_price"),
                        rs.getLong("group_price"),
                        rs.getInt("status"),
                        rs.getInt("stock_available")
                )
                        : null,
                goodsId
        );
        if (base == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "goods not found");
        }

        long originalPriceCents = toCentsOrDefault(request == null ? null : request.getOriginalPriceYuan(), base.originalPriceCents());
        long groupPriceCents = base.groupPriceCents();
        if (request != null && request.getDiscountRate() != null) {
            double discountRate = request.getDiscountRate();
            if (discountRate <= 0 || discountRate > 10) {
                throw new BizException(ErrorCode.INVALID_PARAM, "discountRate must be in (0, 10]");
            }
            groupPriceCents = Math.max(1L, Math.round(originalPriceCents * (discountRate / 10D)));
        }
        groupPriceCents = Math.min(groupPriceCents, originalPriceCents);

        int goodsStatus = normalizeStatus(request == null ? null : request.getGoodsStatus(), base.goodsStatus(), 0, 2);
        int stockAvailable = normalizeNonNegative(request == null ? null : request.getStockAvailable(), base.stockAvailable());

        jdbcTemplate.update(
                "UPDATE " + TRADE_DB + ".trade_goods SET original_price = ?, group_price = ?, status = ?, stock_available = ?, updated_at = NOW() " +
                        "WHERE goods_id = ?",
                originalPriceCents,
                groupPriceCents,
                goodsStatus,
                stockAvailable,
                goodsId
        );

        ActivityRow activity = queryLatestActivity(goodsId);
        int targetCount = normalizePositive(request == null ? null : request.getTargetCount(), activity == null ? DEFAULT_TARGET_COUNT : activity.targetCount());
        int validMinutes = normalizePositive(request == null ? null : request.getValidMinutes(), activity == null ? DEFAULT_VALID_MINUTES : activity.validMinutes());
        int activityStatus = normalizeStatus(request == null ? null : request.getActivityStatus(), activity == null ? 1 : activity.status(), 0, 3);
        int activityDays = normalizePositive(request == null ? null : request.getActivityDays(), DEFAULT_ACTIVITY_DAYS);

        if (activity == null) {
            long activityId = buildActivityId(base.imageId());
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(
                    "INSERT INTO " + TRADE_DB + ".trade_activity " +
                            "(activity_id, goods_id, activity_name, group_type, take_limit_count, target_count, valid_minutes, status, start_time, end_time, created_by, created_at, updated_at) " +
                            "VALUES (?, ?, ?, 0, 1, ?, ?, ?, ?, ?, NULL, NOW(), NOW())",
                    activityId,
                    goodsId,
                    "图片拼团-" + goodsId,
                    targetCount,
                    validMinutes,
                    activityStatus,
                    now,
                    now.plusDays(activityDays)
            );
        } else {
            LocalDateTime nextEnd = (request != null && request.getActivityDays() != null)
                    ? LocalDateTime.now().plusDays(activityDays)
                    : activity.endTime();
            jdbcTemplate.update(
                    "UPDATE " + TRADE_DB + ".trade_activity SET target_count = ?, valid_minutes = ?, status = ?, end_time = ?, updated_at = NOW() " +
                            "WHERE id = ?",
                    targetCount,
                    validMinutes,
                    activityStatus,
                    nextEnd,
                    activity.id()
            );
        }

        GroupParamRow updated = listGroupParams().stream()
                .filter(item -> goodsId.equals(item.getGoodsId()))
                .findFirst()
                .orElse(null);
        if (updated != null) {
            return updated;
        }
        GroupParamRow row = new GroupParamRow();
        row.setGoodsId(goodsId);
        row.setImageId(base.imageId());
        row.setVehicleId(base.vehicleId());
        row.setImageName(base.title());
        row.setOriginalPriceCents(originalPriceCents);
        row.setGroupPriceCents(groupPriceCents);
        row.setDiscountRate(calcDiscountRate(originalPriceCents, groupPriceCents));
        row.setGoodsStatus(goodsStatus);
        row.setStockAvailable(stockAvailable);
        row.setTargetCount(targetCount);
        row.setValidMinutes(validMinutes);
        row.setActivityStatus(activityStatus);
        return row;
    }

    private ActivityRow queryLatestActivity(String goodsId) {
        return jdbcTemplate.query(
                "SELECT id, activity_id, target_count, valid_minutes, status, end_time FROM " + TRADE_DB + ".trade_activity " +
                        "WHERE goods_id = ? ORDER BY id DESC LIMIT 1",
                rs -> rs.next()
                        ? new ActivityRow(
                        rs.getLong("id"),
                        rs.getLong("activity_id"),
                        rs.getInt("target_count"),
                        rs.getInt("valid_minutes"),
                        rs.getInt("status"),
                        rs.getObject("end_time", LocalDateTime.class)
                )
                        : null,
                goodsId
        );
    }

    private long buildActivityId(Long imageId) {
        if (imageId != null && imageId > 0) {
            return 1_000_000_000_000L + imageId;
        }
        return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(100, 999);
    }

    private int normalizeNonNegative(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(0, value);
    }

    private int normalizePositive(Integer value, int fallback) {
        if (value == null || value <= 0) {
            return fallback;
        }
        return value;
    }

    private int normalizeStatus(Integer value, int fallback, int min, int max) {
        if (value == null) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    private long toCentsOrDefault(Double value, long fallbackCents) {
        if (value == null || value <= 0) {
            return fallbackCents;
        }
        BigDecimal yuan = BigDecimal.valueOf(value);
        return yuan.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private double calcDiscountRate(long originalCents, long groupCents) {
        if (originalCents <= 0) {
            return 0D;
        }
        BigDecimal rate = BigDecimal.valueOf(groupCents)
                .multiply(BigDecimal.TEN)
                .divide(BigDecimal.valueOf(originalCents), 2, RoundingMode.HALF_UP);
        return rate.doubleValue();
    }

    private record GroupBaseRow(
            String goodsId,
            Long imageId,
            Long vehicleId,
            String title,
            long originalPriceCents,
            long groupPriceCents,
            int goodsStatus,
            int stockAvailable
    ) {
    }

    private record ActivityRow(
            long id,
            long activityId,
            int targetCount,
            int validMinutes,
            int status,
            LocalDateTime endTime
    ) {
    }

    @Data
    public static class GroupParamUpdateRequest {
        private Double originalPriceYuan;
        private Double discountRate;
        private Integer targetCount;
        private Integer validMinutes;
        private Integer goodsStatus;
        private Integer stockAvailable;
        private Integer activityStatus;
        private Integer activityDays;
    }

    @Data
    public static class GroupParamRow {
        private String goodsId;
        private Long imageId;
        private Long vehicleId;
        private String imageName;
        private Long originalPriceCents;
        private Long groupPriceCents;
        private Double discountRate;
        private Integer goodsStatus;
        private Integer stockAvailable;
        private Long activityId;
        private Integer targetCount;
        private Integer validMinutes;
        private Integer activityStatus;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
