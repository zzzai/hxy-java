SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 服务订单履约骨架（支付后入队）增量脚本
-- 日期：2026-02-23
-- 目的：
-- 1) 建立服务履约主表，承接“支付成功 -> 待预约”最小闭环
-- 2) 通过 order_item_id 唯一键保证幂等
-- =============================================

CREATE TABLE IF NOT EXISTS `trade_service_order`
(
    `id`            bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`      bigint      NOT NULL COMMENT '交易订单 ID',
    `order_no`      varchar(64) NOT NULL COMMENT '交易订单号',
    `order_item_id` bigint      NOT NULL COMMENT '交易订单项 ID',
    `user_id`       bigint      NOT NULL COMMENT '用户 ID',
    `pay_order_id`  bigint               DEFAULT NULL COMMENT '支付单 ID',
    `spu_id`        bigint      NOT NULL COMMENT '商品 SPU ID',
    `sku_id`        bigint      NOT NULL COMMENT '商品 SKU ID',
    `status`        tinyint     NOT NULL DEFAULT '0' COMMENT '履约状态: 0 待预约 10 已预约 20 服务中 30 已完成 40 已取消',
    `source`        varchar(32) NOT NULL DEFAULT 'PAY_CALLBACK' COMMENT '创建来源',
    `booking_no`    varchar(64) NOT NULL DEFAULT '' COMMENT '预约单号',
    `remark`        varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
    `creator`       varchar(64)          DEFAULT '' COMMENT '创建者',
    `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`       varchar(64)          DEFAULT '' COMMENT '更新者',
    `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_item_id` (`order_item_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) COMMENT ='服务履约单表';

SET FOREIGN_KEY_CHECKS = 1;
