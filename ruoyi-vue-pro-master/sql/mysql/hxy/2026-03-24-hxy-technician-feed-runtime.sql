SET NAMES utf8mb4;

-- HXY: Technician Feed runtime tables (Reserved / 技师动态)

CREATE TABLE IF NOT EXISTS technician_feed_post (
    id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    title VARCHAR(128) DEFAULT NULL,
    content VARCHAR(500) NOT NULL,
    cover_url VARCHAR(512) DEFAULT NULL,
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    published_at DATETIME NOT NULL,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_tf_post_store_technician (store_id, technician_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS technician_feed_like (
    id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    client_token VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    liked_at DATETIME DEFAULT NULL,
    canceled_at DATETIME DEFAULT NULL,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_tf_like_post_member (post_id, member_id, status, id),
    KEY idx_tf_like_client_token (client_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS technician_feed_comment (
    id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    client_token VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'REVIEWING',
    degraded BIT(1) NOT NULL DEFAULT b'0',
    submitted_at DATETIME NOT NULL,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_tf_comment_post (post_id, status, id),
    KEY idx_tf_comment_client_token (client_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
