SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- HXY: 门店 SKU 库存流水（幂等键：biz_type + biz_no + store_id + sku_id）

CREATE TABLE IF NOT EXISTS `hxy_store_product_sku_stock_flow`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `biz_type`        varchar(64)  NOT NULL COMMENT '业务类型',
    `biz_no`          varchar(64)  NOT NULL COMMENT '业务单号',
    `store_id`        bigint       NOT NULL COMMENT '门店编号',
    `sku_id`          bigint       NOT NULL COMMENT 'SKU 编号',
    `incr_count`      int          NOT NULL COMMENT '库存变化值：正数加库存，负数减库存',
    `status`          tinyint      NOT NULL DEFAULT '0' COMMENT '状态：0待执行 1成功 2失败 3执行中',
    `retry_count`     int          NOT NULL DEFAULT '0' COMMENT '重试次数',
    `next_retry_time` datetime     NULL     DEFAULT NULL COMMENT '下次重试时间',
    `last_error_msg`  varchar(255) NOT NULL DEFAULT '' COMMENT '最后错误信息',
    `execute_time`    datetime     NULL     DEFAULT NULL COMMENT '最近执行时间',
    `last_retry_operator` varchar(64) NOT NULL DEFAULT '' COMMENT '最近重试操作人',
    `last_retry_source`   varchar(32) NOT NULL DEFAULT '' COMMENT '最近重试来源',
    `creator`         varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`         varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`       bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_stock_biz` (`tenant_id`, `biz_type`, `biz_no`, `store_id`, `sku_id`),
    KEY `idx_store_sku` (`store_id`, `sku_id`),
    KEY `idx_status_retry` (`status`, `next_retry_time`)
) COMMENT ='门店 SKU 库存流水表';

SET FOREIGN_KEY_CHECKS = 1;
