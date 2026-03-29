package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.config.TradeBridgeProperties;
import com.busgallery.busgallery.dto.response.TradeBindingResponse;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.User;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.mapper.VehicleImageMapper;
import com.busgallery.busgallery.repository.UserRepository;
import com.busgallery.busgallery.service.ImageAccessService;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.TradeBridgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Trade bridge service implementation using trade_center tables in the same MySQL instance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TradeBridgeServiceImpl implements TradeBridgeService {

    private static final String TRADE_DB = "trade_center";
    private static final long ACTIVITY_ID_BASE = 1_000_000_000_000L;

    private final JdbcTemplate jdbcTemplate;
    private final ImageService imageService;
    private final ImageAccessService imageAccessService;
    private final VehicleImageMapper vehicleImageMapper;
    private final UserRepository userRepository;
    private final TradeBridgeProperties properties;

    @Override
    @Transactional
    public TradeBindingResponse resolveOrCreateByImageId(Long imageId, Long preferredVehicleId) {
        if (imageId == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "imageId is required");
        }
        Image image = imageService.findRawById(imageId);
        if (image == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Image not found");
        }

        try {
            Long vehicleId = preferredVehicleId != null ? preferredVehicleId : vehicleImageMapper.selectPrimaryVehicleIdByImageId(imageId);
            String goodsId = resolveOrCreateGoods(image, vehicleId);
            ActivityRow activity = resolveOrCreateActivity(goodsId, imageId);
            GoodsRow goods = queryGoods(goodsId);
            int activeTeamCount = countActiveTeam(activity.activityId());

            return TradeBindingResponse.builder()
                    .imageId(imageId)
                    .vehicleId(goods.vehicleId() != null ? goods.vehicleId() : vehicleId)
                    .goodsId(goodsId)
                    .activityId(activity.activityId())
                    .goodsTitle(goods.title())
                    .coverUrl(buildCoverUrl(goods.coverUrl(), goods.imageId()))
                    .originalPriceCents(goods.originalPrice())
                    .groupPriceCents(goods.groupPrice())
                    .targetCount(activity.targetCount())
                    .validMinutes(activity.validMinutes())
                    .activeTeamCount(activeTeamCount)
                    .build();
        } catch (DataAccessException ex) {
            log.error("Trade bridge DB error for imageId={}", imageId, ex);
            throw new BizException(ErrorCode.BUSINESS_ERROR, "trade_center is unavailable or not initialized");
        }
    }

    @Override
    public TradeBindingResponse queryByGoodsId(String goodsId) {
        if (!StringUtils.hasText(goodsId)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "goodsId is required");
        }
        try {
            GoodsRow goods = queryGoods(goodsId.trim());
            if (goods == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "Trade goods not found");
            }
            Long resolvedImageId = goods.imageId() != null
                    ? goods.imageId()
                    : queryPrimaryImageIdByVehicleId(goods.vehicleId());
            if (goods.imageId() == null && resolvedImageId != null) {
                jdbcTemplate.update(
                        "UPDATE " + TRADE_DB + ".trade_goods SET image_id = ?, updated_at = NOW() " +
                                "WHERE goods_id = ? AND image_id IS NULL",
                        resolvedImageId,
                        goods.goodsId()
                );
            }
            ActivityRow activity = queryActiveActivityByGoodsId(goods.goodsId());
            if (activity == null) {
                activity = queryLatestActivityByGoodsId(goods.goodsId());
            }
            int activeTeamCount = activity == null ? 0 : countActiveTeam(activity.activityId());
            return TradeBindingResponse.builder()
                    .imageId(resolvedImageId)
                    .vehicleId(goods.vehicleId())
                    .goodsId(goods.goodsId())
                    .activityId(activity == null ? null : activity.activityId())
                    .goodsTitle(goods.title())
                    .coverUrl(buildCoverUrl(goods.coverUrl(), resolvedImageId))
                    .originalPriceCents(goods.originalPrice())
                    .groupPriceCents(goods.groupPrice())
                    .targetCount(activity == null ? null : activity.targetCount())
                    .validMinutes(activity == null ? null : activity.validMinutes())
                    .activeTeamCount(activeTeamCount)
                    .build();
        } catch (BizException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            log.error("Trade bridge DB error for goodsId={}", goodsId, ex);
            throw new BizException(ErrorCode.BUSINESS_ERROR, "trade_center is unavailable or not initialized");
        }
    }

    private String resolveOrCreateGoods(Image image, Long vehicleId) {
        String existingGoodsId = queryGoodsIdByImageId(image.getId());
        if (StringUtils.hasText(existingGoodsId)) {
            if (vehicleId != null) {
                jdbcTemplate.update(
                        "UPDATE " + TRADE_DB + ".trade_goods SET vehicle_id = COALESCE(vehicle_id, ?) WHERE goods_id = ?",
                        vehicleId, existingGoodsId
                );
            }
            return existingGoodsId;
        }

        String goodsId = buildGoodsId(image.getId());
        String tradeUserId = ensureTradeUserMap(image);
        String title = buildGoodsTitle(image, vehicleId);
        String coverUrl = pickCoverObjectRef(image);
        long originalPrice = normalizePositive(properties.getDefaultOriginalPriceCents(), 19900L);
        long groupPrice = normalizePositive(properties.getDefaultGroupPriceCents(), 14900L);
        if (groupPrice > originalPrice) {
            groupPrice = originalPrice;
        }
        int stock = normalizePositive(properties.getDefaultStock(), 9999);

        jdbcTemplate.update(
                "INSERT INTO " + TRADE_DB + ".trade_goods " +
                        "(goods_id, image_id, vehicle_id, seller_user_id, seller_trade_user_id, title, cover_url, " +
                        "original_price, group_price, stock_total, stock_available, status, audit_status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, NOW(), NOW()) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "vehicle_id = COALESCE(VALUES(vehicle_id), vehicle_id), " +
                        "seller_user_id = VALUES(seller_user_id), " +
                        "seller_trade_user_id = VALUES(seller_trade_user_id), " +
                        "title = VALUES(title), " +
                        "cover_url = VALUES(cover_url), " +
                        "updated_at = NOW()",
                goodsId,
                image.getId(),
                vehicleId,
                normalizePositive(image.getUploaderId(), 0L),
                tradeUserId,
                title,
                coverUrl,
                originalPrice,
                groupPrice,
                stock,
                stock
        );
        return goodsId;
    }

    private ActivityRow resolveOrCreateActivity(String goodsId, Long imageId) {
        ActivityRow active = queryActiveActivityByGoodsId(goodsId);
        if (active != null) {
            return active;
        }

        int targetCount = normalizePositive(properties.getDefaultTargetCount(), 3);
        int validMinutes = normalizePositive(properties.getDefaultValidMinutes(), 1440);
        int activityDays = normalizePositive(properties.getDefaultActivityDays(), 365);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(activityDays);

        for (int i = 0; i < 5; i++) {
            long activityId = buildActivityId(imageId, i);
            int rows = jdbcTemplate.update(
                    "INSERT IGNORE INTO " + TRADE_DB + ".trade_activity " +
                            "(activity_id, goods_id, activity_name, group_type, take_limit_count, target_count, valid_minutes, " +
                            "status, start_time, end_time, created_by, created_at, updated_at) " +
                            "VALUES (?, ?, ?, 0, 1, ?, ?, 1, ?, ?, NULL, NOW(), NOW())",
                    activityId,
                    goodsId,
                    "图片拼团-" + goodsId,
                    targetCount,
                    validMinutes,
                    now,
                    end
            );
            if (rows > 0) {
                return new ActivityRow(activityId, targetCount, validMinutes);
            }
            ActivityRow found = queryActivityById(activityId);
            if (found != null && StringUtils.hasText(found.goodsId()) && found.goodsId().equals(goodsId)) {
                return found;
            }
        }

        ActivityRow fallback = queryActiveActivityByGoodsId(goodsId);
        if (fallback != null) {
            return fallback;
        }
        throw new BizException(ErrorCode.BUSINESS_ERROR, "Unable to create trade activity");
    }

    private String ensureTradeUserMap(Image image) {
        Long appUserId = image.getUploaderId();
        if (appUserId == null || appUserId <= 0) {
            return null;
        }
        String tradeUserId = String.format("TU_%010d", appUserId);
        String username = trimToNull(image.getUploaderUsername());
        String displayName = trimToNull(image.getUploaderDisplayName());

        if (!StringUtils.hasText(username) || !StringUtils.hasText(displayName)) {
            User user = userRepository.findById(appUserId).orElse(null);
            if (user != null) {
                if (!StringUtils.hasText(username)) {
                    username = trimToNull(user.getUsername());
                }
                if (!StringUtils.hasText(displayName)) {
                    displayName = trimToNull(user.getDisplayName());
                }
            }
        }

        jdbcTemplate.update(
                "INSERT INTO " + TRADE_DB + ".trade_user_map " +
                        "(app_user_id, trade_user_id, username_snapshot, display_name_snapshot, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 1, NOW(), NOW()) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "username_snapshot = COALESCE(VALUES(username_snapshot), username_snapshot), " +
                        "display_name_snapshot = COALESCE(VALUES(display_name_snapshot), display_name_snapshot), " +
                        "updated_at = NOW()",
                appUserId,
                tradeUserId,
                username,
                displayName
        );
        return tradeUserId;
    }

    private String queryGoodsIdByImageId(Long imageId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT goods_id FROM " + TRADE_DB + ".trade_goods WHERE image_id = ? LIMIT 1",
                    String.class,
                    imageId
            );
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private GoodsRow queryGoods(String goodsId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT goods_id, image_id, vehicle_id, title, cover_url, original_price, group_price " +
                            "FROM " + TRADE_DB + ".trade_goods WHERE goods_id = ? LIMIT 1",
                    (rs, rowNum) -> new GoodsRow(
                            rs.getString("goods_id"),
                            rs.getObject("image_id", Long.class),
                            rs.getObject("vehicle_id", Long.class),
                            rs.getString("title"),
                            rs.getString("cover_url"),
                            rs.getLong("original_price"),
                            rs.getLong("group_price")
                    ),
                    goodsId
            );
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private ActivityRow queryActiveActivityByGoodsId(String goodsId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT activity_id, goods_id, target_count, valid_minutes FROM " + TRADE_DB + ".trade_activity " +
                            "WHERE goods_id = ? AND status = 1 AND start_time <= NOW() AND end_time >= NOW() " +
                            "ORDER BY id DESC LIMIT 1",
                    (rs, rowNum) -> new ActivityRow(
                            rs.getLong("activity_id"),
                            rs.getString("goods_id"),
                            rs.getInt("target_count"),
                            rs.getInt("valid_minutes")
                    ),
                    goodsId
            );
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private ActivityRow queryActivityById(Long activityId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT activity_id, goods_id, target_count, valid_minutes FROM " + TRADE_DB + ".trade_activity " +
                            "WHERE activity_id = ? LIMIT 1",
                    (rs, rowNum) -> new ActivityRow(
                            rs.getLong("activity_id"),
                            rs.getString("goods_id"),
                            rs.getInt("target_count"),
                            rs.getInt("valid_minutes")
                    ),
                    activityId
            );
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private ActivityRow queryLatestActivityByGoodsId(String goodsId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT activity_id, goods_id, target_count, valid_minutes FROM " + TRADE_DB + ".trade_activity " +
                            "WHERE goods_id = ? ORDER BY id DESC LIMIT 1",
                    (rs, rowNum) -> new ActivityRow(
                            rs.getLong("activity_id"),
                            rs.getString("goods_id"),
                            rs.getInt("target_count"),
                            rs.getInt("valid_minutes")
                    ),
                    goodsId
            );
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private int countActiveTeam(Long activityId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM " + TRADE_DB + ".trade_order_team WHERE activity_id = ? AND status = 0",
                Integer.class,
                activityId
        );
        return count == null ? 0 : count;
    }

    private Long queryPrimaryImageIdByVehicleId(Long vehicleId) {
        if (vehicleId == null || vehicleId <= 0) {
            return null;
        }
        try {
            return jdbcTemplate.query(
                    "SELECT image_id FROM vehicle_image WHERE vehicle_id = ? " +
                            "ORDER BY is_cover DESC, sort_order ASC, image_id DESC LIMIT 1",
                    rs -> rs.next() ? rs.getLong("image_id") : null,
                    vehicleId
            );
        } catch (DataAccessException ignored) {
            return null;
        }
    }

    private String pickCoverObjectRef(Image image) {
        if (image == null) {
            return null;
        }
        String thumbnailRef = imageAccessService.resolveObjectNameRef(image.getThumbnailUrl());
        if (StringUtils.hasText(thumbnailRef)) {
            return thumbnailRef;
        }
        String displayRef = imageAccessService.resolveObjectNameRef(image.getUrl());
        if (StringUtils.hasText(displayRef)) {
            return displayRef;
        }
        String originalRef = imageAccessService.resolveObjectNameRef(image.getObjectName());
        if (StringUtils.hasText(originalRef)) {
            return originalRef;
        }
        return null;
    }

    private String buildCoverUrl(String coverRef, Long imageId) {
        String objectRef = imageAccessService.resolveObjectNameRef(coverRef);
        if (StringUtils.hasText(objectRef)) {
            try {
                return imageAccessService.signThumbnailObject(objectRef);
            } catch (Exception ignored) {
                // fallback below
            }
        }
        if (imageId != null && imageId > 0) {
            Image image = imageService.findById(imageId);
            if (image != null) {
                if (StringUtils.hasText(image.getThumbnailUrl())) {
                    return image.getThumbnailUrl();
                }
                if (StringUtils.hasText(image.getUrl())) {
                    return image.getUrl();
                }
            }
        }
        return null;
    }

    private String buildGoodsId(Long imageId) {
        return "G" + imageId;
    }

    private long buildActivityId(Long imageId, int attempt) {
        if (attempt == 0 && imageId != null && imageId > 0) {
            return ACTIVITY_ID_BASE + imageId;
        }
        long random = ThreadLocalRandom.current().nextLong(100, 999);
        return System.currentTimeMillis() * 1000L + random;
    }

    private String buildGoodsTitle(Image image, Long vehicleId) {
        String plate = null;
        if (vehicleId != null) {
            try {
                plate = jdbcTemplate.queryForObject(
                        "SELECT plate_number FROM vehicle WHERE id = ? LIMIT 1",
                        String.class,
                        vehicleId
                );
            } catch (Exception ignored) {
                // ignore
            }
        }
        if (StringUtils.hasText(plate)) {
            return plate + " 图片商品";
        }
        return "图片商品#" + image.getId();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private long normalizePositive(Long value, long fallback) {
        if (value == null || value <= 0) {
            return fallback;
        }
        return value;
    }

    private int normalizePositive(Integer value, int fallback) {
        if (value == null || value <= 0) {
            return fallback;
        }
        return value;
    }

    private record GoodsRow(String goodsId, Long imageId, Long vehicleId, String title, String coverUrl, long originalPrice, long groupPrice) {
    }

    private record ActivityRow(Long activityId, String goodsId, int targetCount, int validMinutes) {
        private ActivityRow(Long activityId, int targetCount, int validMinutes) {
            this(activityId, null, targetCount, validMinutes);
        }
    }
}
