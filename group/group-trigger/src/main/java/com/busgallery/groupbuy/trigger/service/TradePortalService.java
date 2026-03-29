package com.busgallery.groupbuy.trigger.service;

import com.busgallery.groupbuy.api.dto.PortalCheckoutRequest;
import com.busgallery.groupbuy.api.dto.PortalCheckoutResponse;
import com.busgallery.groupbuy.api.dto.RefundOrderResponse;
import com.busgallery.groupbuy.api.dto.PortalTeamSummaryResponse;
import com.busgallery.groupbuy.api.dto.PortalUserMessageResponse;
import com.busgallery.groupbuy.api.dto.PortalUserRecordResponse;
import com.busgallery.groupbuy.domain.model.LockOrderProjection;
import com.busgallery.groupbuy.domain.model.SettleOrderProjection;
import com.busgallery.groupbuy.domain.service.TradeDomainService;
import com.busgallery.groupbuy.types.enums.GroupErrorCode;
import com.busgallery.groupbuy.types.exception.GroupBizException;
import com.busgallery.groupbuy.types.model.IdFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * User-facing trade workflow service for direct purchase and group purchase.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradePortalService {

    private final JdbcTemplate jdbcTemplate;
    private final TradeDomainService tradeDomainService;
    private volatile boolean portalTablesReady = false;

    /**
     * Query running teams under one activity.
     *
     * @param activityId activity id
     * @param limit      result size
     * @return team list
     */
    public List<PortalTeamSummaryResponse> listActiveTeams(Long activityId, int limit) {
        ensurePortalTables();
        if (activityId == null) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(20, limit));
        return jdbcTemplate.query(
                "SELECT team_id, target_count, complete_count, lock_count, valid_end_time " +
                        "FROM trade_order_team " +
                        "WHERE activity_id = ? AND status = 0 AND lock_count > 0 AND valid_end_time > NOW() " +
                        "ORDER BY created_at DESC LIMIT ?",
                (rs, rowNum) -> PortalTeamSummaryResponse.builder()
                        .teamId(rs.getString("team_id"))
                        .targetCount(rs.getInt("target_count"))
                        .completeCount(rs.getInt("complete_count"))
                        .lockCount(rs.getInt("lock_count"))
                        .validEndTime(toLocalDateTime(rs.getTimestamp("valid_end_time")))
                        .build(),
                activityId,
                safeLimit
        );
    }

    /**
     * Execute direct-buy checkout (pay now, download available immediately).
     *
     * @param userId   current user id
     * @param request  checkout request
     * @return checkout response
     */
    @Transactional
    public PortalCheckoutResponse directCheckout(Long userId, PortalCheckoutRequest request) {
        ensurePortalTables();
        validateCheckoutRequest(userId, request);
        String outTradeNo = nextOutTradeNo("DIR");

        LockOrderProjection lock = tradeDomainService.lockOrder(
                userId,
                null,
                request.getActivityId(),
                request.getGoodsId(),
                request.getSource(),
                request.getChannel(),
                outTradeNo,
                2
        );
        debitBalance(userId, lock.getPayPriceCents());
        SettleOrderProjection settle = tradeDomainService.settleOrder(userId, outTradeNo, LocalDateTime.now());

        OrderRow row = findOrderByOutTradeNo(outTradeNo);
        String recordId = upsertUserRecord(row, 1, true);
        upsertUserMessage(
                userId,
                "DIRECT_SUCCESS",
                "下单成功",
                "支付成功，原图已可下载。",
                recordId
        );

        return PortalCheckoutResponse.builder()
                .outTradeNo(outTradeNo)
                .orderId(settle.getOrderId())
                .teamId(settle.getTeamId())
                .orderStatus(settle.getOrderStatus())
                .teamStatus(resolveTeamStatusText(settle.getTeamId()))
                .payPriceCents(lock.getPayPriceCents())
                .recordId(recordId)
                .canDownload(true)
                .waitingGroup(false)
                .message("支付成功，已解锁原图下载")
                .build();
    }

    /**
     * Execute group-buy checkout (pay now, then wait for group success or timeout refund).
     *
     * @param userId   current user id
     * @param request  checkout request
     * @return checkout response
     */
    @Transactional
    public PortalCheckoutResponse groupCheckout(Long userId, PortalCheckoutRequest request) {
        ensurePortalTables();
        validateCheckoutRequest(userId, request);
        String outTradeNo = nextOutTradeNo("GRP");

        LockOrderProjection lock = tradeDomainService.lockOrder(
                userId,
                StringUtils.hasText(request.getTeamId()) ? request.getTeamId().trim() : null,
                request.getActivityId(),
                request.getGoodsId(),
                request.getSource(),
                request.getChannel(),
                outTradeNo,
                1
        );
        debitBalance(userId, lock.getPayPriceCents());
        SettleOrderProjection settle = tradeDomainService.settleOrder(userId, outTradeNo, LocalDateTime.now());

        String teamStatus = resolveTeamStatusText(settle.getTeamId());
        OrderRow row = findOrderByOutTradeNo(outTradeNo);
        boolean teamSucceeded = "SUCCESS".equals(teamStatus);
        String recordId;
        if (teamSucceeded) {
            refreshTeamSuccessRecords(settle.getTeamId());
            recordId = findRecordIdByOrderId(settle.getOrderId());
            upsertUserMessage(
                    userId,
                    "GROUP_SUCCESS",
                    "拼团成功",
                    "恭喜成团，已解锁原图下载。",
                    recordId
            );
        } else {
            recordId = upsertUserRecord(row, 0, false);
            upsertUserMessage(
                    userId,
                    "GROUP_WAIT",
                    "拼团进行中",
                    "支付成功，等待其他用户拼团成功后可下载原图。",
                    recordId
            );
        }

        return PortalCheckoutResponse.builder()
                .outTradeNo(outTradeNo)
                .orderId(settle.getOrderId())
                .teamId(settle.getTeamId())
                .orderStatus(settle.getOrderStatus())
                .teamStatus(teamStatus)
                .payPriceCents(lock.getPayPriceCents())
                .recordId(recordId)
                .canDownload(teamSucceeded)
                .waitingGroup(!teamSucceeded)
                .message(teamSucceeded ? "拼团成功，已解锁下载" : "已支付，等待成团")
                .build();
    }

    /**
     * List user messages.
     *
     * @param userId current user id
     * @param limit  limit
     * @return message list
     */
    public List<PortalUserMessageResponse> listUserMessages(Long userId, int limit) {
        ensurePortalTables();
        if (userId == null) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(100, limit));
        return jdbcTemplate.query(
                "SELECT message_id, message_type, title, content, biz_record_id, created_at " +
                        "FROM trade_user_message WHERE app_user_id = ? ORDER BY created_at DESC LIMIT ?",
                (rs, rowNum) -> PortalUserMessageResponse.builder()
                        .messageId(rs.getString("message_id"))
                        .messageType(rs.getString("message_type"))
                        .title(rs.getString("title"))
                        .content(rs.getString("content"))
                        .bizRecordId(rs.getString("biz_record_id"))
                        .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                        .build(),
                userId,
                safeLimit
        );
    }

    /**
     * List user trade records.
     *
     * @param userId current user id
     * @param limit  limit
     * @return record list
     */
    public List<PortalUserRecordResponse> listUserRecords(Long userId, int limit) {
        ensurePortalTables();
        if (userId == null) {
            return List.of();
        }
        refreshUserRecordSnapshots(userId);
        int safeLimit = Math.max(1, Math.min(200, limit));
        return jdbcTemplate.query(
                "SELECT r.record_id, r.order_id, r.out_trade_no, r.team_id, r.goods_id, r.image_id, r.vehicle_id, " +
                        "r.order_mode, r.trade_status, r.pay_price, r.can_download, r.created_at, " +
                        "g.title AS goods_title, g.cover_url " +
                        "FROM trade_user_record r " +
                        "LEFT JOIN trade_goods g ON g.goods_id = r.goods_id " +
                        "WHERE r.app_user_id = ? ORDER BY r.created_at DESC LIMIT ?",
                (rs, rowNum) -> {
                    String recordId = rs.getString("record_id");
                    return PortalUserRecordResponse.builder()
                            .recordId(recordId)
                            .orderId(rs.getString("order_id"))
                            .outTradeNo(rs.getString("out_trade_no"))
                            .teamId(rs.getString("team_id"))
                            .goodsId(rs.getString("goods_id"))
                            .imageId(toNullableLong(rs.getObject("image_id")))
                            .vehicleId(toNullableLong(rs.getObject("vehicle_id")))
                            .title(rs.getString("goods_title"))
                            .coverUrl(rs.getString("cover_url"))
                            .orderMode(rs.getInt("order_mode"))
                            .tradeStatus(rs.getInt("trade_status"))
                            .payPriceCents(rs.getLong("pay_price"))
                            .canDownload(rs.getInt("can_download") == 1)
                            .downloadUrl("/api/trade-bridge/purchases/" + recordId + "/download")
                            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                            .build();
                },
                userId,
                safeLimit
        );
    }

    /**
     * Manual refund initiated by user cancel action.
     * This method keeps wallet balance and user record state consistent.
     *
     * @param userId     current user id
     * @param outTradeNo external trade no
     * @return refund result
     */
    @Transactional
    public RefundOrderResponse refundAndSyncRecord(Long userId, String outTradeNo) {
        ensurePortalTables();
        if (userId == null) {
            throw new GroupBizException(GroupErrorCode.UNAUTHORIZED, "login required");
        }
        if (!StringUtils.hasText(outTradeNo)) {
            throw new GroupBizException(GroupErrorCode.INVALID_PARAM, "outTradeNo is required");
        }

        OrderStatusRow statusRow = findOrderStatusByOutTradeNo(outTradeNo);
        if (statusRow == null) {
            throw new GroupBizException(GroupErrorCode.NOT_FOUND, "order not found");
        }
        if (!userId.equals(statusRow.userId())) {
            throw new GroupBizException(GroupErrorCode.CONFLICT, "order user mismatch");
        }

        boolean alreadyRefunded = statusRow.status() == 2;
        if (!alreadyRefunded) {
            tradeDomainService.refundOrder(userId, outTradeNo);
            // Balance payment is used in portal checkout, refund to wallet once.
            if (statusRow.status() == 1 && statusRow.payPrice() > 0) {
                creditBalance(userId, statusRow.payPrice());
            }
        }

        OrderRow row = findOrderByOutTradeNo(outTradeNo);
        String recordId = upsertUserRecord(row, 2, false);
        cleanupEmptyTeam(row.teamId());
        upsertUserMessage(
                userId,
                "GROUP_CANCELLED",
                "取消拼团成功",
                alreadyRefunded ? "该订单已退款，无需重复操作。" : "订单已取消，金额已原路退回到钱包。",
                recordId
        );

        return RefundOrderResponse.builder()
                .userId(String.valueOf(userId))
                .orderId(row.orderId())
                .teamId(row.teamId())
                .code("REFUND_SUCCESS")
                .message(alreadyRefunded ? "already refunded" : "refund processed")
                .build();
    }

    /**
     * Scheduler: expire grouping teams, refund all paid orders, and notify users.
     */
    @Scheduled(fixedDelayString = "${group.trade.team-timeout-check-delay-ms:30000}")
    @Transactional
    public void handleTimeoutTeams() {
        ensurePortalTables();
        List<String> expiredTeams = jdbcTemplate.query(
                "SELECT team_id FROM trade_order_team WHERE status = 0 AND valid_end_time < NOW() ORDER BY valid_end_time ASC LIMIT 100",
                (rs, rowNum) -> rs.getString("team_id")
        );
        for (String teamId : expiredTeams) {
            try {
                failTeamAndRefund(teamId);
            } catch (Exception ex) {
                log.error("fail team timeout handling error, teamId={}, reason={}", teamId, ex.getMessage(), ex);
            }
        }
    }

    @Transactional
    public void failTeamAndRefund(String teamId) {
        ensurePortalTables();
        if (!StringUtils.hasText(teamId)) {
            return;
        }
        int changed = jdbcTemplate.update(
                "UPDATE trade_order_team SET status = 2, updated_at = NOW() WHERE team_id = ? AND status = 0",
                teamId
        );
        if (changed <= 0) {
            return;
        }
        List<OrderRow> paidOrders = findPaidOrdersByTeamId(teamId);
        for (OrderRow row : paidOrders) {
            int updated = jdbcTemplate.update(
                    "UPDATE trade_order_item SET status = 2, refund_time = NOW(), updated_at = NOW() " +
                            "WHERE order_id = ? AND status = 1",
                    row.orderId()
            );
            if (updated <= 0) {
                continue;
            }
            creditBalance(row.userId(), row.payPrice());
            String recordId = upsertUserRecord(row, 2, false);
            upsertUserMessage(
                    row.userId(),
                    "GROUP_FAILED",
                    "拼团失败",
                    "拼团超时未成团，资金已原路退回。",
                    recordId
            );
        }
    }

    @Transactional
    public void refreshTeamSuccessRecords(String teamId) {
        ensurePortalTables();
        List<OrderRow> paidOrders = findPaidOrdersByTeamId(teamId);
        for (OrderRow row : paidOrders) {
            String recordId = upsertUserRecord(row, 1, true);
            upsertUserMessage(
                    row.userId(),
                    "GROUP_SUCCESS",
                    "拼团成功",
                    "团队已成团，原图已可下载。",
                    recordId
            );
        }
    }

    private void validateCheckoutRequest(Long userId, PortalCheckoutRequest request) {
        if (userId == null) {
            throw new GroupBizException(GroupErrorCode.UNAUTHORIZED, "login required");
        }
        if (request == null || !StringUtils.hasText(request.getGoodsId()) || request.getActivityId() == null) {
            throw new GroupBizException(GroupErrorCode.INVALID_PARAM, "goodsId/activityId is required");
        }
    }

    private synchronized void ensurePortalTables() {
        if (portalTablesReady) {
            return;
        }
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS trade_user_record (" +
                        "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT," +
                        "record_id VARCHAR(40) NOT NULL," +
                        "app_user_id BIGINT UNSIGNED NOT NULL," +
                        "order_id VARCHAR(32) NOT NULL," +
                        "out_trade_no VARCHAR(64) NOT NULL," +
                        "team_id VARCHAR(32) NOT NULL," +
                        "activity_id BIGINT UNSIGNED NOT NULL," +
                        "goods_id VARCHAR(32) NOT NULL," +
                        "image_id BIGINT UNSIGNED DEFAULT NULL," +
                        "vehicle_id BIGINT UNSIGNED DEFAULT NULL," +
                        "order_mode TINYINT NOT NULL DEFAULT 1," +
                        "trade_status TINYINT NOT NULL DEFAULT 0," +
                        "pay_price BIGINT NOT NULL DEFAULT 0," +
                        "can_download TINYINT NOT NULL DEFAULT 0," +
                        "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (id)," +
                        "UNIQUE KEY uk_trade_user_record_record_id (record_id)," +
                        "UNIQUE KEY uk_trade_user_record_order_id (order_id)," +
                        "KEY idx_trade_user_record_user_created (app_user_id, created_at)," +
                        "KEY idx_trade_user_record_goods (goods_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS trade_user_message (" +
                        "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT," +
                        "message_id VARCHAR(40) NOT NULL," +
                        "app_user_id BIGINT UNSIGNED NOT NULL," +
                        "message_type VARCHAR(32) NOT NULL," +
                        "title VARCHAR(128) NOT NULL," +
                        "content VARCHAR(512) NOT NULL," +
                        "biz_record_id VARCHAR(40) DEFAULT NULL," +
                        "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (id)," +
                        "UNIQUE KEY uk_trade_user_message_message_id (message_id)," +
                        "UNIQUE KEY uk_trade_user_message_dedup (app_user_id, message_type, biz_record_id)," +
                        "KEY idx_trade_user_message_user_created (app_user_id, created_at)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
        portalTablesReady = true;
    }

    private String resolveTeamStatusText(String teamId) {
        if (!StringUtils.hasText(teamId)) {
            return "GROUPING";
        }
        Integer status = jdbcTemplate.query(
                "SELECT status FROM trade_order_team WHERE team_id = ? LIMIT 1",
                rs -> rs.next() ? rs.getInt("status") : null,
                teamId
        );
        if (status == null) {
            return "GROUPING";
        }
        if (status == 1) {
            return "SUCCESS";
        }
        if (status == 2) {
            return "FAILED";
        }
        if (status == 3) {
            return "CLOSED";
        }
        return "GROUPING";
    }

    private String nextOutTradeNo(String prefix) {
        return prefix + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(10000, 99999);
    }

    private void debitBalance(Long userId, long amount) {
        long safeAmount = Math.max(0L, amount);
        int updated = jdbcTemplate.update(
                "UPDATE bus_gallery.app_user SET balance_cents = balance_cents - ? WHERE id = ?",
                safeAmount,
                userId
        );
        if (updated <= 0) {
            throw new GroupBizException(GroupErrorCode.NOT_FOUND, "user not found");
        }
    }

    private void creditBalance(Long userId, long amount) {
        long safeAmount = Math.max(0L, amount);
        int updated = jdbcTemplate.update(
                "UPDATE bus_gallery.app_user SET balance_cents = balance_cents + ? WHERE id = ?",
                safeAmount,
                userId
        );
        if (updated <= 0) {
            throw new GroupBizException(GroupErrorCode.NOT_FOUND, "user not found");
        }
    }

    private void refreshUserRecordSnapshots(Long userId) {
        jdbcTemplate.update(
                "UPDATE trade_user_record r " +
                        "JOIN trade_order_team t ON t.team_id = r.team_id " +
                        "SET r.trade_status = 1, r.can_download = 1, r.updated_at = NOW() " +
                        "WHERE r.app_user_id = ? AND r.order_mode = 1 AND r.trade_status = 0 AND t.status = 1",
                userId
        );

        jdbcTemplate.update(
                "UPDATE trade_user_record r " +
                        "LEFT JOIN trade_order_team t ON t.team_id = r.team_id " +
                        "SET r.trade_status = 2, r.can_download = 0, r.updated_at = NOW() " +
                        "WHERE r.app_user_id = ? AND r.order_mode = 1 AND r.trade_status = 0 " +
                        "AND (t.team_id IS NULL OR t.status IN (2, 3) OR (t.status = 0 AND t.lock_count <= 0))",
                userId
        );
    }

    private void cleanupEmptyTeam(String teamId) {
        if (!StringUtils.hasText(teamId)) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE trade_order_team t SET " +
                        "lock_count = (SELECT COUNT(1) FROM trade_order_item i WHERE i.team_id = t.team_id AND i.status IN (0, 1)), " +
                        "complete_count = (SELECT COUNT(1) FROM trade_order_item i WHERE i.team_id = t.team_id AND i.status = 1), " +
                        "updated_at = NOW() " +
                        "WHERE t.team_id = ?",
                teamId
        );
        jdbcTemplate.update(
                "DELETE FROM trade_order_team WHERE team_id = ? AND lock_count <= 0 AND complete_count <= 0",
                teamId
        );
    }

    private OrderRow findOrderByOutTradeNo(String outTradeNo) {
        return jdbcTemplate.queryForObject(
                "SELECT i.order_id, i.out_trade_no, i.team_id, i.activity_id, i.goods_id, i.user_id, i.order_mode, i.pay_price, " +
                        "g.image_id, g.vehicle_id, g.title, g.cover_url " +
                        "FROM trade_order_item i " +
                        "LEFT JOIN trade_goods g ON g.goods_id = i.goods_id " +
                        "WHERE i.out_trade_no = ? LIMIT 1",
                (rs, rowNum) -> new OrderRow(
                        rs.getString("order_id"),
                        rs.getString("out_trade_no"),
                        rs.getString("team_id"),
                        rs.getLong("activity_id"),
                        rs.getString("goods_id"),
                        rs.getLong("user_id"),
                        rs.getInt("order_mode"),
                        rs.getLong("pay_price"),
                        toNullableLong(rs.getObject("image_id")),
                        toNullableLong(rs.getObject("vehicle_id")),
                        rs.getString("title"),
                        rs.getString("cover_url")
                ),
                outTradeNo
        );
    }

    private OrderStatusRow findOrderStatusByOutTradeNo(String outTradeNo) {
        return jdbcTemplate.query(
                "SELECT user_id, status, pay_price FROM trade_order_item WHERE out_trade_no = ? LIMIT 1 FOR UPDATE",
                rs -> rs.next()
                        ? new OrderStatusRow(
                        rs.getLong("user_id"),
                        rs.getInt("status"),
                        rs.getLong("pay_price")
                )
                        : null,
                outTradeNo
        );
    }

    private List<OrderRow> findPaidOrdersByTeamId(String teamId) {
        return jdbcTemplate.query(
                "SELECT i.order_id, i.out_trade_no, i.team_id, i.activity_id, i.goods_id, i.user_id, i.order_mode, i.pay_price, " +
                        "g.image_id, g.vehicle_id, g.title, g.cover_url " +
                        "FROM trade_order_item i " +
                        "LEFT JOIN trade_goods g ON g.goods_id = i.goods_id " +
                        "WHERE i.team_id = ? AND i.status = 1",
                (rs, rowNum) -> new OrderRow(
                        rs.getString("order_id"),
                        rs.getString("out_trade_no"),
                        rs.getString("team_id"),
                        rs.getLong("activity_id"),
                        rs.getString("goods_id"),
                        rs.getLong("user_id"),
                        rs.getInt("order_mode"),
                        rs.getLong("pay_price"),
                        toNullableLong(rs.getObject("image_id")),
                        toNullableLong(rs.getObject("vehicle_id")),
                        rs.getString("title"),
                        rs.getString("cover_url")
                ),
                teamId
        );
    }

    private String upsertUserRecord(OrderRow row, int tradeStatus, boolean canDownload) {
        String recordId = findRecordIdByOrderId(row.orderId());
        if (!StringUtils.hasText(recordId)) {
            recordId = IdFactory.nextRecordId();
        }
        jdbcTemplate.update(
                "INSERT INTO trade_user_record " +
                        "(record_id, app_user_id, order_id, out_trade_no, team_id, activity_id, goods_id, image_id, vehicle_id, order_mode, trade_status, pay_price, can_download, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "out_trade_no = VALUES(out_trade_no), team_id = VALUES(team_id), activity_id = VALUES(activity_id), " +
                        "goods_id = VALUES(goods_id), image_id = VALUES(image_id), vehicle_id = VALUES(vehicle_id), " +
                        "order_mode = VALUES(order_mode), trade_status = VALUES(trade_status), pay_price = VALUES(pay_price), " +
                        "can_download = VALUES(can_download), updated_at = NOW()",
                recordId,
                row.userId(),
                row.orderId(),
                row.outTradeNo(),
                row.teamId(),
                row.activityId(),
                row.goodsId(),
                row.imageId(),
                row.vehicleId(),
                row.orderMode(),
                tradeStatus,
                row.payPrice(),
                canDownload ? 1 : 0
        );
        return recordId;
    }

    private String findRecordIdByOrderId(String orderId) {
        return jdbcTemplate.query(
                "SELECT record_id FROM trade_user_record WHERE order_id = ? LIMIT 1",
                rs -> rs.next() ? rs.getString("record_id") : null,
                orderId
        );
    }

    private void upsertUserMessage(Long userId, String messageType, String title, String content, String recordId) {
        String messageId = IdFactory.nextMessageId();
        jdbcTemplate.update(
                "INSERT INTO trade_user_message (message_id, app_user_id, message_type, title, content, biz_record_id, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content), created_at = NOW()",
                messageId,
                userId,
                messageType,
                title,
                content,
                recordId
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private Long toNullableLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private record OrderRow(
            String orderId,
            String outTradeNo,
            String teamId,
            Long activityId,
            String goodsId,
            Long userId,
            int orderMode,
            long payPrice,
            Long imageId,
            Long vehicleId,
            String title,
            String coverUrl
    ) {
    }

    private record OrderStatusRow(
            Long userId,
            int status,
            long payPrice
    ) {
    }
}
