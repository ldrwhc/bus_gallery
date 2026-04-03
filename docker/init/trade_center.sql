-- =========================================================
-- trade_center initial schema (DDD trade domain)
-- Notes:
-- 1) Trade domain uses independent database: trade_center.
-- 2) User relation uses app_user_id from bus_gallery.app_user.id (no cross-DB FK).
-- 3) Persist username/display_name snapshot in orders for history stability.
-- 4) All money fields store integer cents (amount * 100).
-- =========================================================

CREATE DATABASE IF NOT EXISTS `trade_center`
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

USE `trade_center`;

SET NAMES utf8mb4;

-- user mapping: content user -> trade user
DROP TABLE IF EXISTS `trade_user_map`;
CREATE TABLE `trade_user_map` (
  `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `app_user_id`           BIGINT UNSIGNED NOT NULL COMMENT 'bus_gallery.app_user.id',
  `trade_user_id`         VARCHAR(64)     NOT NULL COMMENT 'trade user code, e.g. TU_202603280001',
  `username_snapshot`     VARCHAR(64)     DEFAULT NULL,
  `display_name_snapshot` VARCHAR(128)    DEFAULT NULL,
  `status`                TINYINT         NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_user_map_app_user` (`app_user_id`),
  UNIQUE KEY `uk_trade_user_map_trade_user` (`trade_user_id`),
  KEY `idx_trade_user_map_username` (`username_snapshot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='trade user mapping';

-- goods table: one image as one tradable goods
DROP TABLE IF EXISTS `trade_goods`;
CREATE TABLE `trade_goods` (
  `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `goods_id`             VARCHAR(32)     NOT NULL COMMENT 'business goods id',
  `image_id`             BIGINT UNSIGNED NOT NULL COMMENT 'bus_gallery.image.id',
  `vehicle_id`           BIGINT UNSIGNED DEFAULT NULL COMMENT 'bus_gallery.vehicle.id',
  `seller_user_id`       BIGINT UNSIGNED NOT NULL COMMENT 'seller app_user_id',
  `seller_trade_user_id` VARCHAR(64)     DEFAULT NULL,
  `title`                VARCHAR(255)    NOT NULL,
  `cover_url`            VARCHAR(1024)   DEFAULT NULL,
  `original_price`       BIGINT          NOT NULL COMMENT 'unit: cent',
  `group_price`          BIGINT          NOT NULL COMMENT 'unit: cent',
  `stock_total`          INT UNSIGNED    NOT NULL DEFAULT 0,
  `stock_available`      INT UNSIGNED    NOT NULL DEFAULT 0,
  `status`               TINYINT         NOT NULL DEFAULT 1 COMMENT '0 offline, 1 online, 2 frozen',
  `audit_status`         TINYINT         NOT NULL DEFAULT 1 COMMENT '0 pending, 1 passed, 2 rejected',
  `ext_json`             JSON            DEFAULT NULL,
  `created_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_goods_goods_id` (`goods_id`),
  UNIQUE KEY `uk_trade_goods_image_id` (`image_id`),
  KEY `idx_trade_goods_vehicle` (`vehicle_id`),
  KEY `idx_trade_goods_seller` (`seller_user_id`),
  KEY `idx_trade_goods_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='trade goods';

-- group activity table
DROP TABLE IF EXISTS `trade_activity`;
CREATE TABLE `trade_activity` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `activity_id`      BIGINT UNSIGNED NOT NULL COMMENT 'business activity id',
  `goods_id`         VARCHAR(32)     NOT NULL COMMENT 'trade_goods.goods_id',
  `activity_name`    VARCHAR(128)    NOT NULL,
  `group_type`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 normal, 1 ladder',
  `take_limit_count` INT             NOT NULL DEFAULT 1,
  `target_count`     INT             NOT NULL DEFAULT 3,
  `valid_minutes`    INT             NOT NULL DEFAULT 15,
  `status`           TINYINT         NOT NULL DEFAULT 1 COMMENT '0 draft, 1 active, 2 ended, 3 closed',
  `start_time`       DATETIME        NOT NULL,
  `end_time`         DATETIME        NOT NULL,
  `created_by`       BIGINT UNSIGNED DEFAULT NULL COMMENT 'creator app_user_id',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_activity_activity_id` (`activity_id`),
  KEY `idx_trade_activity_goods_status` (`goods_id`, `status`),
  KEY `idx_trade_activity_time_window` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='group activity';

-- team-level order table
DROP TABLE IF EXISTS `trade_order_team`;
CREATE TABLE `trade_order_team` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `team_id`             VARCHAR(32)     NOT NULL COMMENT 'business team id',
  `activity_id`         BIGINT UNSIGNED NOT NULL,
  `goods_id`            VARCHAR(32)     NOT NULL,
  `owner_user_id`       BIGINT UNSIGNED NOT NULL,
  `owner_trade_user_id` VARCHAR(64)     DEFAULT NULL,
  `original_price`      BIGINT          NOT NULL COMMENT 'unit: cent',
  `deduction_price`     BIGINT          NOT NULL DEFAULT 0 COMMENT 'unit: cent',
  `pay_price`           BIGINT          NOT NULL COMMENT 'unit: cent',
  `target_count`        INT             NOT NULL,
  `lock_count`          INT             NOT NULL DEFAULT 0,
  `complete_count`      INT             NOT NULL DEFAULT 0,
  `status`              TINYINT         NOT NULL DEFAULT 0 COMMENT '0 grouping, 1 success, 2 failed, 3 closed',
  `valid_start_time`    DATETIME        NOT NULL,
  `valid_end_time`      DATETIME        NOT NULL,
  `notify_type`         VARCHAR(16)     NOT NULL DEFAULT 'MQ',
  `notify_url`          VARCHAR(512)    DEFAULT NULL,
  `version`             INT UNSIGNED    NOT NULL DEFAULT 0,
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_order_team_team_id` (`team_id`),
  KEY `idx_trade_order_team_activity_status` (`activity_id`, `status`),
  KEY `idx_trade_order_team_goods_status` (`goods_id`, `status`),
  KEY `idx_trade_order_team_valid_end` (`valid_end_time`),
  KEY `idx_team_status_end_team` (`status`, `valid_end_time`, `team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='team order';

-- user-level order table
DROP TABLE IF EXISTS `trade_order_item`;
CREATE TABLE `trade_order_item` (
  `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id`              VARCHAR(32)     NOT NULL COMMENT 'business order id',
  `team_id`               VARCHAR(32)     NOT NULL,
  `activity_id`           BIGINT UNSIGNED NOT NULL,
  `goods_id`              VARCHAR(32)     NOT NULL,
  `user_id`               BIGINT UNSIGNED NOT NULL COMMENT 'app_user_id',
  `trade_user_id`         VARCHAR(64)     DEFAULT NULL,
  `username_snapshot`     VARCHAR(64)     DEFAULT NULL,
  `display_name_snapshot` VARCHAR(128)    DEFAULT NULL,
  `order_mode`            TINYINT         NOT NULL DEFAULT 1 COMMENT '1 group, 2 solo',
  `source`                VARCHAR(32)     NOT NULL DEFAULT 'content',
  `channel`               VARCHAR(32)     NOT NULL DEFAULT 'web',
  `original_price`        BIGINT          NOT NULL COMMENT 'unit: cent',
  `deduction_price`       BIGINT          NOT NULL DEFAULT 0 COMMENT 'unit: cent',
  `pay_price`             BIGINT          NOT NULL COMMENT 'unit: cent',
  `status`                TINYINT         NOT NULL DEFAULT 0 COMMENT '0 locked, 1 paid, 2 refunded, 3 cancelled, 4 timeout',
  `out_trade_no`          VARCHAR(64)     NOT NULL COMMENT 'external trade no for idempotency',
  `out_trade_time`        DATETIME        DEFAULT NULL,
  `pay_success_time`      DATETIME        DEFAULT NULL,
  `refund_time`           DATETIME        DEFAULT NULL,
  `biz_id`                VARCHAR(64)     NOT NULL COMMENT 'business unique id',
  `idempotency_key`       VARCHAR(128)    DEFAULT NULL,
  `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_order_item_order_id` (`order_id`),
  UNIQUE KEY `uk_trade_order_item_out_trade_no` (`out_trade_no`),
  UNIQUE KEY `uk_trade_order_item_biz_id` (`biz_id`),
  KEY `idx_trade_order_item_user_status` (`user_id`, `status`),
  KEY `idx_trade_order_item_team_status` (`team_id`, `status`),
  KEY `idx_item_team_status_mode` (`team_id`, `status`, `order_mode`),
  KEY `idx_trade_order_item_activity_status` (`activity_id`, `status`),
  KEY `idx_trade_order_item_goods_status` (`goods_id`, `status`),
  KEY `idx_trade_order_item_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='order item';

-- notify task table
DROP TABLE IF EXISTS `trade_notify_task`;
CREATE TABLE `trade_notify_task` (
  `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `task_id`            VARCHAR(32)     NOT NULL,
  `biz_type`           VARCHAR(32)     NOT NULL COMMENT 'ORDER_SETTLED/TEAM_SUCCESS/REFUND_SUCCESS',
  `biz_id`             VARCHAR(64)     NOT NULL,
  `notify_category`    VARCHAR(32)     NOT NULL DEFAULT 'ORDER',
  `notify_type`        VARCHAR(16)     NOT NULL COMMENT 'MQ/HTTP',
  `notify_mq`          VARCHAR(128)    DEFAULT NULL,
  `notify_routing_key` VARCHAR(128)    DEFAULT NULL,
  `notify_url`         VARCHAR(512)    DEFAULT NULL,
  `payload_json`       JSON            NOT NULL,
  `notify_status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '0 pending, 1 success, 2 failed, 3 abandoned',
  `notify_count`       INT UNSIGNED    NOT NULL DEFAULT 0,
  `next_retry_at`      DATETIME        DEFAULT NULL,
  `last_error`         VARCHAR(512)    DEFAULT NULL,
  `created_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_notify_task_task_id` (`task_id`),
  UNIQUE KEY `uk_trade_notify_task_biz` (`biz_type`, `biz_id`, `notify_type`),
  KEY `idx_trade_notify_task_status_retry` (`notify_status`, `next_retry_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='notify task';

-- outbox table for reliable event publishing
DROP TABLE IF EXISTS `trade_outbox_event`;
CREATE TABLE `trade_outbox_event` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `event_id`       VARCHAR(64)     NOT NULL,
  `aggregate_type` VARCHAR(32)     NOT NULL COMMENT 'GOODS/TEAM/ORDER',
  `aggregate_id`   VARCHAR(64)     NOT NULL,
  `event_type`     VARCHAR(64)     NOT NULL COMMENT 'ORDER_LOCKED/ORDER_PAID/TEAM_COMPLETED/ORDER_REFUNDED',
  `event_key`      VARCHAR(128)    NOT NULL DEFAULT '',
  `payload_json`   JSON            NOT NULL,
  `headers_json`   JSON            DEFAULT NULL,
  `publish_status` TINYINT         NOT NULL DEFAULT 0 COMMENT '0 pending, 1 published, 2 failed',
  `retry_count`    INT UNSIGNED    NOT NULL DEFAULT 0,
  `next_retry_at`  DATETIME        DEFAULT NULL,
  `published_at`   DATETIME        DEFAULT NULL,
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_outbox_event_event_id` (`event_id`),
  UNIQUE KEY `uk_trade_outbox_event_dedup` (`aggregate_type`, `aggregate_id`, `event_type`, `event_key`),
  KEY `idx_trade_outbox_event_publish` (`publish_status`, `next_retry_at`),
  KEY `idx_trade_outbox_event_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='outbox event';

-- user-visible trade records
DROP TABLE IF EXISTS `trade_user_record`;
CREATE TABLE `trade_user_record` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `record_id`        VARCHAR(40)     NOT NULL COMMENT 'business record id',
  `app_user_id`      BIGINT UNSIGNED NOT NULL COMMENT 'bus_gallery.app_user.id',
  `order_id`         VARCHAR(32)     NOT NULL,
  `out_trade_no`     VARCHAR(64)     NOT NULL,
  `team_id`          VARCHAR(32)     NOT NULL,
  `activity_id`      BIGINT UNSIGNED NOT NULL,
  `goods_id`         VARCHAR(32)     NOT NULL,
  `image_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT 'bus_gallery.image.id',
  `vehicle_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT 'bus_gallery.vehicle.id',
  `order_mode`       TINYINT         NOT NULL DEFAULT 1 COMMENT '1 group, 2 direct',
  `trade_status`     TINYINT         NOT NULL DEFAULT 0 COMMENT '0 waiting_group, 1 success, 2 group_failed_refunded, 3 refunded, 4 failed',
  `pay_price`        BIGINT          NOT NULL DEFAULT 0 COMMENT 'unit: cent',
  `can_download`     TINYINT         NOT NULL DEFAULT 0 COMMENT '1 yes, 0 no',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_user_record_record_id` (`record_id`),
  UNIQUE KEY `uk_trade_user_record_order_id` (`order_id`),
  KEY `idx_record_mode_status_user_record_team` (`order_mode`, `trade_status`, `app_user_id`, `record_id`, `team_id`),
  KEY `idx_trade_user_record_user_created` (`app_user_id`, `created_at`),
  KEY `idx_trade_user_record_goods` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user trade record';

-- user-visible trade message inbox
DROP TABLE IF EXISTS `trade_user_message`;
CREATE TABLE `trade_user_message` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `message_id`       VARCHAR(40)     NOT NULL COMMENT 'business message id',
  `app_user_id`      BIGINT UNSIGNED NOT NULL COMMENT 'bus_gallery.app_user.id',
  `message_type`     VARCHAR(32)     NOT NULL COMMENT 'DIRECT_SUCCESS/GROUP_WAIT/GROUP_SUCCESS/GROUP_FAILED',
  `title`            VARCHAR(128)    NOT NULL,
  `content`          VARCHAR(512)    NOT NULL,
  `biz_record_id`    VARCHAR(40)     DEFAULT NULL,
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_user_message_message_id` (`message_id`),
  UNIQUE KEY `uk_trade_user_message_dedup` (`app_user_id`, `message_type`, `biz_record_id`),
  KEY `idx_trade_user_message_user_created` (`app_user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user trade message';
