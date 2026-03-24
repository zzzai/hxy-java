SET NAMES utf8mb4;

-- HXY: Gift Card runtime tables (Reserved / 礼品卡)

CREATE TABLE IF NOT EXISTS gift_card_template (
    id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    face_value INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    valid_days INT NOT NULL DEFAULT 30,
    status TINYINT NOT NULL DEFAULT 1,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_gift_card_template_status (status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gift_card_order (
    id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    send_scene VARCHAR(16) NOT NULL,
    receiver_member_id BIGINT DEFAULT NULL,
    client_token VARCHAR(128) NOT NULL,
    gift_card_batch_no VARCHAR(64) NOT NULL,
    amount_total INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    degraded BIT(1) NOT NULL DEFAULT b'0',
    degrade_reason VARCHAR(128) DEFAULT '',
    refund_reason VARCHAR(255) DEFAULT NULL,
    pay_refund_id VARCHAR(64) DEFAULT NULL,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_gift_card_order_member (member_id, id),
    KEY idx_gift_card_order_template (template_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gift_card (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    card_no VARCHAR(64) NOT NULL,
    redeem_code VARCHAR(32) NOT NULL,
    receiver_member_id BIGINT DEFAULT NULL,
    status VARCHAR(32) NOT NULL,
    valid_end_time DATETIME DEFAULT NULL,
    redeemed_at DATETIME DEFAULT NULL,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_gift_card_no (card_no),
    KEY idx_gift_card_order (order_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gift_card_redeem_record (
    id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    card_no VARCHAR(64) NOT NULL,
    client_token VARCHAR(128) NOT NULL,
    redeemed_at DATETIME NOT NULL,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_gift_card_redeem_card (card_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
