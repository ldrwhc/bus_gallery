DROP DATABASE IF EXISTS `bus_gallery`;
CREATE DATABASE `bus_gallery`
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

USE `bus_gallery`;

-- 品牌
CREATE TABLE `brand` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`          VARCHAR(128)    NOT NULL COMMENT '品牌名称',
  `chn_name`      varchar(200)    DEFAULT NULL COMMENT '品牌中文',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_brand_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆品牌';

-- 运营公司
CREATE TABLE `company` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(128)    NOT NULL,
  `region_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '所属地区',
  `description`   TEXT            DEFAULT NULL,
  `logo_url`      VARCHAR(512)    DEFAULT NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客运/公交公司';

-- 地区
CREATE TABLE `region` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(128)    NOT NULL,
  `parent_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '父级地区',
  `level`       TINYINT         DEFAULT 0 COMMENT '层级（国家/省/市…）',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_region_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地区信息';

-- 车型
CREATE TABLE `model` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `brand_id`      BIGINT UNSIGNED NOT NULL,
  `name`          VARCHAR(128)    NOT NULL,
  `model_code`    VARCHAR(64)     DEFAULT NULL COMMENT '厂内型号/代码',
  `description`   TEXT            DEFAULT NULL,
  `release_year`  SMALLINT        DEFAULT NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_model_brand` (`brand_id`),
  CONSTRAINT `fk_model_brand` FOREIGN KEY (`brand_id`) REFERENCES `brand` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车型信息';

-- 车辆主体
CREATE TABLE `vehicle` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `model_id`       BIGINT UNSIGNED NOT NULL,
  `company_id`     BIGINT UNSIGNED DEFAULT NULL,
  `region_id`      BIGINT UNSIGNED DEFAULT NULL,
  `plate_number`   VARCHAR(64)     DEFAULT NULL COMMENT '车牌号',
  `custom_number`  VARCHAR(64)     DEFAULT NULL COMMENT '公司内部编号',
  `factory_date`   DATE            DEFAULT NULL COMMENT '出厂日期',
  `launch_date`    DATE            DEFAULT NULL COMMENT '上牌/投运日期',
  `air_conditioned` TINYINT(1)     DEFAULT 0 COMMENT '是否空调车',
  `source`         VARCHAR(256)    DEFAULT NULL COMMENT '信息来源',
  `remark`         TEXT            DEFAULT NULL,
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_vehicle_model` (`model_id`),
  KEY `idx_vehicle_company` (`company_id`),
  KEY `idx_vehicle_region` (`region_id`),
  CONSTRAINT `fk_vehicle_model` FOREIGN KEY (`model_id`) REFERENCES `model` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_vehicle_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `fk_vehicle_region` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='具体车辆';

-- 车辆配置（动力/底盘等）
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆配置详情';

-- 图片资源
CREATE TABLE `image` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `object_name`    VARCHAR(512)    NOT NULL COMMENT 'MinIO 对象名',
  `url`            VARCHAR(1024)   NOT NULL COMMENT '原图 URL',
  `thumbnail_url`  VARCHAR(1024)   DEFAULT NULL COMMENT '缩略图 URL',
  `size_bytes`     BIGINT          DEFAULT NULL COMMENT '文件大小',
  `width`          INT             DEFAULT NULL,
  `height`         INT             DEFAULT NULL,
  `content_type`   VARCHAR(128)    DEFAULT NULL,
  `hash`           VARCHAR(128)    DEFAULT NULL COMMENT '文件哈希',
  `uploader`       VARCHAR(128)    DEFAULT NULL,
  `uploader_id`    BIGINT UNSIGNED DEFAULT NULL,
  `uploader_username` VARCHAR(64)  DEFAULT NULL,
  `uploader_display_name` VARCHAR(128) DEFAULT NULL,
  `exif_json`      TEXT            DEFAULT NULL COMMENT 'EXIF 元数据',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_image_object` (`object_name`),
  KEY `idx_image_uploader` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片文件信息';

-- 车辆与图片关联
CREATE TABLE `vehicle_image` (
  `vehicle_id`  BIGINT UNSIGNED NOT NULL,
  `image_id`    BIGINT UNSIGNED NOT NULL,
  `is_cover`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否封面图',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆图片关联表';

-- 用户信息
CREATE TABLE `app_user` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`        VARCHAR(64)     NOT NULL COMMENT '登录账号',
  `password_hash`   VARCHAR(255)    NOT NULL COMMENT '加密后的密码',
  `display_name`    VARCHAR(128)    NOT NULL COMMENT '展示名称',
  `avatar_url`      VARCHAR(512)    DEFAULT NULL COMMENT '头像',
  `bio`             VARCHAR(512)    DEFAULT NULL COMMENT '个人简介',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账户';
