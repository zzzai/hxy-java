SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 服务/实物统一商品模型增量脚本
-- 日期：2026-02-22
-- 目的：
-- 1) 在 SPU/订单项上增加商品类型，支撑“服务 + 实物 + 卡项 + 虚拟”统一建模
-- 2) 增加服务加项配置表，支撑“基础价 + 加项价”动态定价
-- =============================================

ALTER TABLE `product_spu`
    ADD COLUMN IF NOT EXISTS `product_type` tinyint NOT NULL DEFAULT '1'
        COMMENT '商品类型: 1 实物 2 服务 3 卡项 4 虚拟' AFTER `status`;

ALTER TABLE `trade_order_item`
    ADD COLUMN IF NOT EXISTS `product_type` tinyint NOT NULL DEFAULT '1'
        COMMENT '商品类型: 1 实物 2 服务 3 卡项 4 虚拟' AFTER `spu_name`;

CREATE TABLE IF NOT EXISTS `hxy_service_addon`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `spu_id`      bigint      NOT NULL COMMENT '服务商品 SPU ID',
    `addon_name`  varchar(64) NOT NULL COMMENT '加项名称',
    `addon_type`  tinyint     NOT NULL COMMENT '加项类型: 1 单选 2 多选',
    `is_required` bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否必选',
    `sort`        int         NOT NULL DEFAULT 0 COMMENT '排序',
    `status`      tinyint     NOT NULL DEFAULT 0 COMMENT '状态: 0 启用 1 停用',
    `creator`     varchar(64)          DEFAULT '' COMMENT '创建者',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)          DEFAULT '' COMMENT '更新者',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`   bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_spu_id` (`spu_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) COMMENT ='服务商品加项定义表';

CREATE TABLE IF NOT EXISTS `hxy_service_addon_item`
(
    `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `addon_id`     bigint      NOT NULL COMMENT '所属加项定义 ID',
    `item_name`    varchar(64) NOT NULL COMMENT '选项名称',
    `extra_price`  int         NOT NULL DEFAULT 0 COMMENT '附加金额，单位：分',
    `is_default`   bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否默认项',
    `is_recommend` bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否推荐项',
    `sort`         int         NOT NULL DEFAULT 0 COMMENT '排序',
    `status`       tinyint     NOT NULL DEFAULT 0 COMMENT '状态: 0 启用 1 停用',
    `creator`      varchar(64)          DEFAULT '' COMMENT '创建者',
    `create_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`      varchar(64)          DEFAULT '' COMMENT '更新者',
    `update_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`    bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_addon_id` (`addon_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) COMMENT ='服务商品加项选项表';

SET FOREIGN_KEY_CHECKS = 1;
