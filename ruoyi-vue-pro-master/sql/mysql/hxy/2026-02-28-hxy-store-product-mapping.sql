SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- HXY: 总部 SPU/SKU -> 门店商品映射模型

CREATE TABLE IF NOT EXISTS `hxy_store_product_spu`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `store_id`     bigint       NOT NULL COMMENT '门店编号',
    `spu_id`       bigint       NOT NULL COMMENT '总部 SPU 编号',
    `product_type` tinyint      NOT NULL DEFAULT '1' COMMENT '商品类型：1实物 2服务 3卡项 4虚拟',
    `sale_status`  tinyint      NOT NULL DEFAULT '0' COMMENT '销售状态：0上架 1下架',
    `sort`         int          NOT NULL DEFAULT '0' COMMENT '排序',
    `remark`       varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
    `creator`      varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`      varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`    bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_store_spu` (`tenant_id`, `store_id`, `spu_id`),
    KEY `idx_store_id` (`store_id`),
    KEY `idx_spu_id` (`spu_id`),
    KEY `idx_sale_status` (`sale_status`)
) COMMENT ='门店 SPU 映射表';

CREATE TABLE IF NOT EXISTS `hxy_store_product_sku`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `store_id`     bigint       NOT NULL COMMENT '门店编号',
    `spu_id`       bigint       NOT NULL COMMENT '总部 SPU 编号（冗余）',
    `sku_id`       bigint       NOT NULL COMMENT '总部 SKU 编号',
    `sale_status`  tinyint      NOT NULL DEFAULT '0' COMMENT '销售状态：0上架 1下架',
    `sale_price`   int          NOT NULL DEFAULT '0' COMMENT '门店售价（分）',
    `market_price` int          NOT NULL DEFAULT '0' COMMENT '门店划线价（分）',
    `stock`        int          NOT NULL DEFAULT '0' COMMENT '门店库存',
    `sort`         int          NOT NULL DEFAULT '0' COMMENT '排序',
    `remark`       varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
    `creator`      varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`      varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`    bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_store_sku` (`tenant_id`, `store_id`, `sku_id`),
    KEY `idx_store_id` (`store_id`),
    KEY `idx_spu_id` (`spu_id`),
    KEY `idx_sku_id` (`sku_id`),
    KEY `idx_sale_status` (`sale_status`)
) COMMENT ='门店 SKU 映射表';

SET FOREIGN_KEY_CHECKS = 1;
