DROP DATABASE IF EXISTS `bus_gallery`;
CREATE DATABASE `bus_gallery`
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

USE `bus_gallery`;

-- 鍝佺墝
CREATE TABLE `brand` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `name`          VARCHAR(128)    NOT NULL COMMENT '鍝佺墝鍚嶇О',
  `chn_name`      varchar(200)    DEFAULT NULL COMMENT '鍝佺墝涓枃',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_brand_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='杞﹁締鍝佺墝';

-- 杩愯惀鍏徃
CREATE TABLE `company` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(128)    NOT NULL,
  `region_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '鎵€灞炲湴鍖?,
  `description`   TEXT            DEFAULT NULL,
  `logo_url`      VARCHAR(512)    DEFAULT NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_name` (`name`),
  KEY `idx_company_region_name` (`region_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瀹㈣繍/鍏氦鍏徃';

-- 鍦板尯
CREATE TABLE `region` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(128)    NOT NULL,
  `parent_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '鐖剁骇鍦板尯',
  `level`       TINYINT         DEFAULT 0 COMMENT '灞傜骇锛堝浗瀹?鐪?甯傗€︼級',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_region_name` (`name`),
  KEY `idx_region_parent_name` (`parent_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鍦板尯淇℃伅';

-- 杞﹀瀷
CREATE TABLE `model` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `brand_id`      BIGINT UNSIGNED NOT NULL,
  `name`          VARCHAR(128)    NOT NULL,
  `model_code`    VARCHAR(64)     DEFAULT NULL COMMENT '鍘傚唴鍨嬪彿/浠ｇ爜',
  `description`   TEXT            DEFAULT NULL,
  `release_year`  SMALLINT        DEFAULT NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_model_brand` (`brand_id`),
  KEY `idx_model_brand_name` (`brand_id`, `name`),
  CONSTRAINT `fk_model_brand` FOREIGN KEY (`brand_id`) REFERENCES `brand` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='杞﹀瀷淇℃伅';

-- 杞﹁締涓讳綋
CREATE TABLE `vehicle` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `model_id`       BIGINT UNSIGNED NOT NULL,
  `company_id`     BIGINT UNSIGNED DEFAULT NULL,
  `region_id`      BIGINT UNSIGNED DEFAULT NULL,
  `plate_number`   VARCHAR(64)     DEFAULT NULL COMMENT '杞︾墝鍙?,
  `custom_number`  VARCHAR(64)     DEFAULT NULL COMMENT '鍏徃鍐呴儴缂栧彿',
  `factory_date`   DATE            DEFAULT NULL COMMENT '鍑哄巶鏃ユ湡',
  `launch_date`    DATE            DEFAULT NULL COMMENT '涓婄墝/鎶曡繍鏃ユ湡',
  `view_count`     BIGINT          NOT NULL DEFAULT 0 COMMENT 'visit count',
  `air_conditioned` TINYINT(1)     DEFAULT 0 COMMENT '鏄惁绌鸿皟杞?,
  `source`         VARCHAR(256)    DEFAULT NULL COMMENT '淇℃伅鏉ユ簮',
  `remark`         TEXT            DEFAULT NULL,
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_vehicle_model` (`model_id`),
  KEY `idx_vehicle_company` (`company_id`),
  KEY `idx_vehicle_region` (`region_id`),
  KEY `idx_vehicle_view_count` (`view_count`),
  CONSTRAINT `fk_vehicle_model` FOREIGN KEY (`model_id`) REFERENCES `model` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_vehicle_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `fk_vehicle_region` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鍏蜂綋杞﹁締';

-- 杞﹁締閰嶇疆锛堝姩鍔?搴曠洏绛夛級
CREATE TABLE `vehicle_config` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `vehicle_id`    BIGINT UNSIGNED NOT NULL,
  `brand_id`      BIGINT UNSIGNED DEFAULT NULL,
  `model_id`      BIGINT UNSIGNED DEFAULT NULL,
  `motor`         VARCHAR(128)    DEFAULT NULL,
  `engine`        VARCHAR(128)    DEFAULT NULL,
  `fuel_type`     VARCHAR(64)     DEFAULT NULL,
  `step_type`     VARCHAR(64)     DEFAULT NULL,
  `transmission_system` VARCHAR(128) DEFAULT NULL,
  `suspension`    VARCHAR(128)    DEFAULT NULL,
  `axle`          VARCHAR(128)    DEFAULT NULL,
  `other_configs` TEXT            DEFAULT NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vehicle_config_vehicle` (`vehicle_id`),
  CONSTRAINT `fk_vehicle_config_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_vehicle_config_brand` FOREIGN KEY (`brand_id`) REFERENCES `brand` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `fk_vehicle_config_model` FOREIGN KEY (`model_id`) REFERENCES `model` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='杞﹁締閰嶇疆璇︽儏';

-- 鍥剧墖璧勬簮
CREATE TABLE `image` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `object_name`    VARCHAR(512)    NOT NULL COMMENT 'MinIO 瀵硅薄鍚?,
  `url`            VARCHAR(1024)   NOT NULL COMMENT '鍘熷浘 URL',
  `thumbnail_url`  VARCHAR(1024)   DEFAULT NULL COMMENT '缂╃暐鍥?URL',
  `size_bytes`     BIGINT          DEFAULT NULL COMMENT '鏂囦欢澶у皬',
  `width`          INT             DEFAULT NULL,
  `height`         INT             DEFAULT NULL,
  `content_type`   VARCHAR(128)    DEFAULT NULL,
  `hash`           VARCHAR(128)    DEFAULT NULL COMMENT '鏂囦欢鍝堝笇',
  `uploader`       VARCHAR(128)    DEFAULT NULL,
  `uploader_id`    BIGINT UNSIGNED DEFAULT NULL,
  `uploader_username` VARCHAR(64)  DEFAULT NULL,
  `uploader_display_name` VARCHAR(128) DEFAULT NULL,
  `exif_json`      TEXT            DEFAULT NULL COMMENT 'EXIF 鍏冩暟鎹?,
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_image_object` (`object_name`),
  KEY `idx_image_uploader` (`uploader_id`),
  KEY `idx_image_uploader_created_id` (`uploader_id`, `created_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鍥剧墖鏂囦欢淇℃伅';

-- 杞﹁締涓庡浘鐗囧叧鑱?
CREATE TABLE `vehicle_image` (
  `vehicle_id`  BIGINT UNSIGNED NOT NULL,
  `image_id`    BIGINT UNSIGNED NOT NULL,
  `is_cover`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '鏄惁灏侀潰鍥?,
  `sort_order`  INT             NOT NULL DEFAULT 0,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`vehicle_id`, `image_id`),
  KEY `idx_vehicle_image_image` (`image_id`),
  CONSTRAINT `fk_vehicle_image_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_vehicle_image_image` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='杞﹁締鍥剧墖鍏宠仈琛?;

-- 鐢ㄦ埛淇℃伅
CREATE TABLE `app_user` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`        VARCHAR(64)     NOT NULL COMMENT '鐧诲綍璐﹀彿',
  `password_hash`   VARCHAR(255)    NOT NULL COMMENT '鍔犲瘑鍚庣殑瀵嗙爜',
  `display_name`    VARCHAR(128)    NOT NULL COMMENT '灞曠ず鍚嶇О',
  `avatar_url`      VARCHAR(512)    DEFAULT NULL COMMENT '澶村儚',
  `bio`             VARCHAR(512)    DEFAULT NULL COMMENT '涓汉绠€浠?,
  `balance_cents`   BIGINT          NOT NULL DEFAULT 0 COMMENT '余额（分）',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鐢ㄦ埛璐︽埛';

-- bus route
CREATE TABLE `bus_route` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `route_number`    VARCHAR(32)     NOT NULL COMMENT 'route number, e.g. 1路 / BRT1',
  `route_name`      VARCHAR(200)    DEFAULT NULL COMMENT 'full route name',
  `sub_type`        VARCHAR(16)     DEFAULT NULL COMMENT 'MAIN / INTERVAL / BRANCH / EXPRESS / NIGHT / DIRECT',
  `parent_route_id` BIGINT UNSIGNED DEFAULT NULL COMMENT 'parent route FK (self-ref for branch/interval)',
  `start_stop`      VARCHAR(128)    DEFAULT NULL COMMENT 'up direction start stop',
  `end_stop`        VARCHAR(128)    DEFAULT NULL COMMENT 'up direction end stop',
  `down_start_stop` VARCHAR(128)    DEFAULT NULL COMMENT 'down start stop (NULL=symmetrical)',
  `down_end_stop`   VARCHAR(128)    DEFAULT NULL COMMENT 'down end stop (NULL=symmetrical)',
  `is_loop`         TINYINT(1)      NOT NULL DEFAULT 0 COMMENT 'is circular route',
  `region_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT 'city/region FK',
  `company_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT 'operator company FK',
  `route_type`      VARCHAR(32)     NOT NULL DEFAULT 'REGULAR' COMMENT 'REGULAR/BRT/AIRPORT/TOURIST/COMMUNITY/SUBWAY',
  `line_length_km`  DECIMAL(7,2)    DEFAULT NULL COMMENT 'route length in km',
  `ticket_type`     VARCHAR(32)     DEFAULT NULL COMMENT 'FLAT/SECTIONAL/FREE',
  `ticket_price`    VARCHAR(64)     DEFAULT NULL COMMENT 'fare description',
  `operating_hours` VARCHAR(128)    DEFAULT NULL COMMENT 'operating hours',
  `is_active`       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT 'still in service',
  `first_operated`  DATE            DEFAULT NULL COMMENT 'first day of service',
  `last_operated`   DATE            DEFAULT NULL COMMENT 'last day (if inactive)',
  `remark`          VARCHAR(500)    DEFAULT NULL,
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_route_number_company_sub` (`route_number`, `company_id`, `sub_type`),
  KEY `idx_route_region`  (`region_id`),
  KEY `idx_route_company` (`company_id`),
  KEY `idx_route_type`    (`route_type`),
  KEY `idx_route_number`  (`route_number`),
  KEY `idx_route_active`  (`is_active`, `region_id`),
  KEY `idx_route_parent`  (`parent_route_id`),
  CONSTRAINT `fk_bus_route_region`  FOREIGN KEY (`region_id`)  REFERENCES `region`(`id`)  ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_bus_route_company` FOREIGN KEY (`company_id`) REFERENCES `company`(`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_bus_route_parent`  FOREIGN KEY (`parent_route_id`) REFERENCES `bus_route`(`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- vehicle-route many-to-many
CREATE TABLE `vehicle_route` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `vehicle_id`    BIGINT UNSIGNED NOT NULL,
  `route_id`      BIGINT UNSIGNED NOT NULL,
  `is_current`    TINYINT(1)      NOT NULL DEFAULT 1 COMMENT 'is current route for this vehicle',
  `remark`        VARCHAR(256)    DEFAULT NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vehicle_route` (`vehicle_id`, `route_id`),
  KEY `idx_vr_vehicle` (`vehicle_id`),
  KEY `idx_vr_route`   (`route_id`),
  KEY `idx_vr_current` (`is_current`, `route_id`),
  CONSTRAINT `fk_vr_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_vr_route`   FOREIGN KEY (`route_id`)   REFERENCES `bus_route`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

