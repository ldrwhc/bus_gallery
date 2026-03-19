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

SET @user_role_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_user'
      AND COLUMN_NAME = 'role'
);

SET @ddl_user_role := IF(
        @user_role_exists = 0,
        'ALTER TABLE `app_user` ADD COLUMN `role` VARCHAR(32) NOT NULL DEFAULT ''USER'' AFTER `bio`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_user_role;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @user_review_region_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_user'
      AND COLUMN_NAME = 'review_region_id'
);

SET @ddl_user_review_region := IF(
        @user_review_region_exists = 0,
        'ALTER TABLE `app_user` ADD COLUMN `review_region_id` BIGINT UNSIGNED NULL AFTER `role`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_user_review_region;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @user_email_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_user'
      AND COLUMN_NAME = 'email'
);

SET @ddl_user_email := IF(
        @user_email_exists = 0,
        'ALTER TABLE `app_user` ADD COLUMN `email` VARCHAR(128) NULL AFTER `bio`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_user_email;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @user_email_verified_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_user'
      AND COLUMN_NAME = 'email_verified_at'
);

SET @ddl_user_email_verified := IF(
        @user_email_verified_exists = 0,
        'ALTER TABLE `app_user` ADD COLUMN `email_verified_at` DATETIME NULL AFTER `email`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_user_email_verified;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @user_pwd_changed_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_user'
      AND COLUMN_NAME = 'password_changed_at'
);

SET @ddl_user_pwd_changed := IF(
        @user_pwd_changed_exists = 0,
        'ALTER TABLE `app_user` ADD COLUMN `password_changed_at` DATETIME NULL AFTER `password_hash`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_user_pwd_changed;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @user_email_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_user'
      AND INDEX_NAME = 'uk_app_user_email'
);

SET @ddl_user_email_index := IF(
        @user_email_index_exists = 0,
        'ALTER TABLE `app_user` ADD UNIQUE KEY `uk_app_user_email` (`email`)',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_user_email_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `app_user` SET `role` = 'USER' WHERE `role` IS NULL OR `role` = '';

UPDATE `vehicle_config` SET `fuel_type` = '汽油' WHERE LOWER(TRIM(`fuel_type`)) = 'gasoline';
UPDATE `vehicle_config` SET `fuel_type` = '柴油' WHERE LOWER(TRIM(`fuel_type`)) = 'diesel';
UPDATE `vehicle_config` SET `fuel_type` = '纯电' WHERE LOWER(TRIM(`fuel_type`)) = 'electric';
UPDATE `vehicle_config` SET `fuel_type` = '混动' WHERE LOWER(TRIM(`fuel_type`)) = 'hybrid';
UPDATE `vehicle_config` SET `fuel_type` = '燃气' WHERE LOWER(TRIM(`fuel_type`)) = 'gas';
UPDATE `vehicle_config` SET `fuel_type` = '柴油+电' WHERE LOWER(TRIM(`fuel_type`)) = 'diesel_electric';
UPDATE `vehicle_config` SET `fuel_type` = '压缩天然气' WHERE LOWER(TRIM(`fuel_type`)) = 'cng';
UPDATE `vehicle_config` SET `fuel_type` = '压缩天然气+电' WHERE LOWER(TRIM(`fuel_type`)) = 'cng_electric';
UPDATE `vehicle_config` SET `fuel_type` = '液化天然气' WHERE LOWER(TRIM(`fuel_type`)) = 'lng';
UPDATE `vehicle_config` SET `fuel_type` = '液化天然气+电' WHERE LOWER(TRIM(`fuel_type`)) = 'lng_electric';
UPDATE `vehicle_config` SET `fuel_type` = '压缩氢气+电' WHERE LOWER(TRIM(`fuel_type`)) IN ('hydrogen_electric', 'compressed_hydrogen_electric');
UPDATE `vehicle_config` SET `fuel_type` = '柴油+电' WHERE TRIM(`fuel_type`) = '柴油 + 电';
UPDATE `vehicle_config` SET `fuel_type` = '压缩天然气+电' WHERE TRIM(`fuel_type`) = '压缩天然气 + 电';
UPDATE `vehicle_config` SET `fuel_type` = '液化天然气+电' WHERE TRIM(`fuel_type`) = '液化天然气 + 电';
UPDATE `vehicle_config` SET `fuel_type` = '压缩氢气+电' WHERE TRIM(`fuel_type`) = '压缩氢气 + 电';

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

CREATE TABLE IF NOT EXISTS `vehicle_submission` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `action_type` VARCHAR(32) NOT NULL COMMENT 'CREATE / UPDATE',
    `status` VARCHAR(32) NOT NULL COMMENT 'PENDING / APPROVED / REJECTED',
    `submitter_id` BIGINT UNSIGNED NOT NULL,
    `submitter_username` VARCHAR(64) NOT NULL,
    `submitter_display_name` VARCHAR(128) DEFAULT NULL,
    `reviewer_id` BIGINT UNSIGNED DEFAULT NULL,
    `reviewer_username` VARCHAR(64) DEFAULT NULL,
    `reviewer_display_name` VARCHAR(128) DEFAULT NULL,
    `region_id` BIGINT UNSIGNED DEFAULT NULL,
    `vehicle_id` BIGINT UNSIGNED DEFAULT NULL,
    `image_id` BIGINT UNSIGNED DEFAULT NULL,
    `request_payload` LONGTEXT NOT NULL,
    `review_payload` LONGTEXT DEFAULT NULL,
    `reject_code` VARCHAR(64) DEFAULT NULL,
    `reject_reason` VARCHAR(512) DEFAULT NULL,
    `reviewed_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_vehicle_submission_status_region` (`status`, `region_id`),
    KEY `idx_vehicle_submission_submitter` (`submitter_id`),
    KEY `idx_vehicle_submission_vehicle` (`vehicle_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = 'pending submissions for upload/update review workflow';
