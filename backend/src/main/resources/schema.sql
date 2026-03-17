CREATE TABLE IF NOT EXISTS `app_user` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username`        VARCHAR(64)     NOT NULL COMMENT 'login account',
    `password_hash`   VARCHAR(255)    NOT NULL COMMENT 'hashed password',
    `display_name`    VARCHAR(128)    NOT NULL COMMENT 'display name',
    `avatar_url`      VARCHAR(512)    DEFAULT NULL COMMENT 'avatar url',
    `bio`             VARCHAR(512)    DEFAULT NULL COMMENT 'bio',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_user_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = 'user accounts';

-- MySQL 5.7 does not support ADD COLUMN IF NOT EXISTS, so emulate the guard.
SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vehicle_config'
      AND COLUMN_NAME = 'transmission_system'
);

SET @ddl := IF(
        @column_exists = 0,
        'ALTER TABLE `vehicle_config` ADD COLUMN `transmission_system` VARCHAR(128) DEFAULT NULL COMMENT ''transmission system'' AFTER `step_type`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @image_exif_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'image'
      AND COLUMN_NAME = 'exif_json'
);

SET @ddl_image := IF(
        @image_exif_exists = 0,
        'ALTER TABLE `image` ADD COLUMN `exif_json` TEXT NULL AFTER `uploader_display_name`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_image;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `vehicle_comment` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `vehicle_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `username` VARCHAR(64) NOT NULL,
    `display_name` VARCHAR(128) NULL,
    `content` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_vehicle_comment_vehicle` (`vehicle_id`),
    CONSTRAINT `fk_vehicle_comment_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle`(`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = 'comments on vehicles';

CREATE TABLE IF NOT EXISTS `vehicle_favorite` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `vehicle_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vehicle_favorite_user_vehicle` (`vehicle_id`, `user_id`),
    KEY `idx_vehicle_favorite_vehicle` (`vehicle_id`),
    CONSTRAINT `fk_vehicle_favorite_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle`(`id`),
    CONSTRAINT `fk_vehicle_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = 'user favorites on vehicles';
