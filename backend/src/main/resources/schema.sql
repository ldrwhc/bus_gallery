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

SET @region_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'region'
);

SET @region_province_id_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'region'
      AND COLUMN_NAME = 'province_id'
);

SET @ddl_region_province_id := IF(
        @region_table_exists > 0 AND @region_province_id_exists = 0,
        'ALTER TABLE `region` ADD COLUMN `province_id` BIGINT UNSIGNED NULL AFTER `parent_id`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_region_province_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @region_type_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'region'
      AND COLUMN_NAME = 'region_type'
);

SET @ddl_region_type := IF(
        @region_table_exists > 0 AND @region_type_exists = 0,
        'ALTER TABLE `region` ADD COLUMN `region_type` VARCHAR(16) NULL AFTER `level`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_region_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_region_province_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'region'
      AND INDEX_NAME = 'idx_region_province_id'
);

SET @ddl_idx_region_province := IF(
        @region_table_exists > 0 AND @idx_region_province_exists = 0,
        'ALTER TABLE `region` ADD KEY `idx_region_province_id` (`province_id`)',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_idx_region_province;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_region_type_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'region'
      AND INDEX_NAME = 'idx_region_type'
);

SET @ddl_idx_region_type := IF(
        @region_table_exists > 0 AND @idx_region_type_exists = 0,
        'ALTER TABLE `region` ADD KEY `idx_region_type` (`region_type`)',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_idx_region_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_region_backfill_root := IF(
        @region_table_exists > 0,
        'UPDATE `region`
         SET `province_id` = IFNULL(`province_id`, `id`),
             `region_type` = IFNULL(`region_type`, ''PROVINCE''),
             `level` = IFNULL(NULLIF(`level`, 0), 1)
         WHERE `parent_id` IS NULL',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_region_backfill_root;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_region_backfill_child := IF(
        @region_table_exists > 0,
        'UPDATE `region` r
         LEFT JOIN `region` p ON r.parent_id = p.id
         SET r.`province_id` = IFNULL(r.`province_id`, IFNULL(p.`province_id`, p.`id`)),
             r.`region_type` = IFNULL(r.`region_type`, ''CITY''),
             r.`level` = IFNULL(NULLIF(r.`level`, 0), 2)
         WHERE r.`parent_id` IS NOT NULL',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_region_backfill_child;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_region_backfill_orphan := IF(
        @region_table_exists > 0,
        'UPDATE `region`
         SET `province_id` = IFNULL(`province_id`, `id`),
             `region_type` = IFNULL(`region_type`, ''PROVINCE''),
             `level` = IFNULL(NULLIF(`level`, 0), 1)
         WHERE `province_id` IS NULL',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_region_backfill_orphan;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `app_user` SET `role` = 'USER' WHERE `role` IS NULL OR `role` = '';

SET @ddl_user_review_region_normalize := IF(
        @region_table_exists > 0,
        'UPDATE `app_user` u
         LEFT JOIN `region` r ON u.review_region_id = r.id
         SET u.review_region_id = COALESCE(r.province_id, r.id, u.review_region_id)
         WHERE u.review_region_id IS NOT NULL',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_user_review_region_normalize;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @company_logo_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'company'
      AND COLUMN_NAME = 'logo_url'
);

SET @ddl_drop_company_logo := IF(
        @company_logo_exists > 0,
        'ALTER TABLE `company` DROP COLUMN `logo_url`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_drop_company_logo;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @brand_country_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'brand'
      AND COLUMN_NAME = 'country'
);

SET @ddl_drop_brand_country := IF(
        @brand_country_exists > 0,
        'ALTER TABLE `brand` DROP COLUMN `country`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_drop_brand_country;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @model_code_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'model'
      AND COLUMN_NAME = 'model_code'
);

SET @ddl_drop_model_code := IF(
        @model_code_exists > 0,
        'ALTER TABLE `model` DROP COLUMN `model_code`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_drop_model_code;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @model_release_year_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'model'
      AND COLUMN_NAME = 'release_year'
);

SET @ddl_drop_model_release_year := IF(
        @model_release_year_exists > 0,
        'ALTER TABLE `model` DROP COLUMN `release_year`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_drop_model_release_year;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `vehicle_config` SET `fuel_type` = '柴油' WHERE LOWER(TRIM(`fuel_type`)) = 'diesel';
UPDATE `vehicle_config` SET `fuel_type` = '纯电' WHERE LOWER(TRIM(`fuel_type`)) = 'electric';
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
UPDATE `vehicle_config`
SET `fuel_type` = NULL
WHERE LOWER(TRIM(`fuel_type`)) IN ('gasoline', 'hybrid')
   OR TRIM(`fuel_type`) IN ('汽油', '混动');

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

SET @vs_province_region_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vehicle_submission'
      AND COLUMN_NAME = 'province_region_id'
);

SET @ddl_vs_province_region := IF(
        @vs_province_region_exists = 0,
        'ALTER TABLE `vehicle_submission` ADD COLUMN `province_region_id` BIGINT UNSIGNED NULL AFTER `region_id`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_vs_province_region;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @vs_city_region_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vehicle_submission'
      AND COLUMN_NAME = 'city_region_id'
);

SET @ddl_vs_city_region := IF(
        @vs_city_region_exists = 0,
        'ALTER TABLE `vehicle_submission` ADD COLUMN `city_region_id` BIGINT UNSIGNED NULL AFTER `province_region_id`',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_vs_city_region;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_vs_status_province_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vehicle_submission'
      AND INDEX_NAME = 'idx_vehicle_submission_status_province'
);

SET @ddl_idx_vs_status_province := IF(
        @idx_vs_status_province_exists = 0,
        'ALTER TABLE `vehicle_submission` ADD KEY `idx_vehicle_submission_status_province` (`status`, `province_region_id`)',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_idx_vs_status_province;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_vs_status_city_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vehicle_submission'
      AND INDEX_NAME = 'idx_vehicle_submission_status_city'
);

SET @ddl_idx_vs_status_city := IF(
        @idx_vs_status_city_exists = 0,
        'ALTER TABLE `vehicle_submission` ADD KEY `idx_vehicle_submission_status_city` (`status`, `city_region_id`)',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_idx_vs_status_city;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_vs_backfill_region_fields := IF(
        @region_table_exists > 0,
        'UPDATE `vehicle_submission` s
         LEFT JOIN `region` r ON s.region_id = r.id
         SET s.province_region_id = COALESCE(s.province_region_id, r.province_id, IF(r.parent_id IS NULL, r.id, r.parent_id)),
             s.city_region_id = COALESCE(s.city_region_id, IF(r.parent_id IS NULL, NULL, r.id))
         WHERE s.region_id IS NOT NULL
           AND (s.province_region_id IS NULL OR s.city_region_id IS NULL)',
        'SELECT 1'
    );

PREPARE stmt FROM @ddl_vs_backfill_region_fields;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `vehicle_submission`
SET `region_id` = COALESCE(`region_id`, `city_region_id`, `province_region_id`)
WHERE `region_id` IS NULL;
