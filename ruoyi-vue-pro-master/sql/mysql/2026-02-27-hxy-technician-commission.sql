-- 技师佣金记录表
CREATE TABLE IF NOT EXISTS technician_commission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '佣金记录ID',
    technician_id BIGINT NOT NULL COMMENT '技师ID',
    order_id BIGINT NOT NULL COMMENT '预约订单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    store_id BIGINT DEFAULT NULL COMMENT '门店ID',
    commission_type TINYINT NOT NULL COMMENT '佣金类型 1=基础 2=点钟 3=加钟 4=卡项销售 5=商品 6=好评',
    base_amount INT NOT NULL DEFAULT 0 COMMENT '订单金额（分）',
    commission_rate DECIMAL(5,4) NOT NULL DEFAULT 0 COMMENT '佣金比例',
    commission_amount INT NOT NULL DEFAULT 0 COMMENT '佣金金额（分）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0=待结算 1=已结算 2=已取消',
    settlement_id BIGINT DEFAULT NULL COMMENT '结算单ID',
    settlement_time DATETIME DEFAULT NULL COMMENT '结算时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    INDEX idx_technician_id (technician_id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_store_id (store_id),
    INDEX idx_settlement_id (settlement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师佣金记录';

-- 技师佣金配置表（门店级）
CREATE TABLE IF NOT EXISTS technician_commission_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    store_id BIGINT NOT NULL COMMENT '门店ID',
    commission_type TINYINT NOT NULL COMMENT '佣金类型 1=基础 2=点钟 3=加钟 4=卡项销售 5=商品 6=好评',
    rate DECIMAL(5,4) NOT NULL DEFAULT 0 COMMENT '佣金比例',
    fixed_amount INT NOT NULL DEFAULT 0 COMMENT '固定金额（分）',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_store_type (store_id, commission_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师佣金配置';
