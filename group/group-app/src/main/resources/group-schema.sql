CREATE TABLE IF NOT EXISTS trade_user_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    record_id VARCHAR(40) NOT NULL,
    app_user_id BIGINT UNSIGNED NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    out_trade_no VARCHAR(64) NOT NULL,
    team_id VARCHAR(32) NOT NULL,
    activity_id BIGINT UNSIGNED NOT NULL,
    goods_id VARCHAR(32) NOT NULL,
    image_id BIGINT UNSIGNED DEFAULT NULL,
    vehicle_id BIGINT UNSIGNED DEFAULT NULL,
    order_mode TINYINT NOT NULL DEFAULT 1,
    trade_status TINYINT NOT NULL DEFAULT 0,
    pay_price BIGINT NOT NULL DEFAULT 0,
    can_download TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_user_record_record_id (record_id),
    UNIQUE KEY uk_trade_user_record_order_id (order_id),
    KEY idx_trade_user_record_user_created (app_user_id, created_at),
    KEY idx_trade_user_record_goods (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS trade_user_message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    message_id VARCHAR(40) NOT NULL,
    app_user_id BIGINT UNSIGNED NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(512) NOT NULL,
    biz_record_id VARCHAR(40) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_user_message_message_id (message_id),
    UNIQUE KEY uk_trade_user_message_dedup (app_user_id, message_type, biz_record_id),
    KEY idx_trade_user_message_user_created (app_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
