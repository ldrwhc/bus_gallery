USE `trade_center`;

CREATE TABLE IF NOT EXISTS `trade_user_record` (
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
  KEY `idx_trade_user_record_user_created` (`app_user_id`, `created_at`),
  KEY `idx_trade_user_record_goods` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user trade record';

CREATE TABLE IF NOT EXISTS `trade_user_message` (
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

