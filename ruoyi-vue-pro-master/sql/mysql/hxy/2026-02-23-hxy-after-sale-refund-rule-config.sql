SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 退款分层规则表配置化脚本
-- 日期：2026-02-23
-- 目的：
-- 1) 将退款分层规则落入数据库表，支持在线维护
-- 2) 规则解析优先使用 DB；若无启用规则，则回退 YAML
-- =============================================

CREATE TABLE IF NOT EXISTS `trade_after_sale_refund_rule`
(
    `id`                        bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `enabled`                   bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `auto_refund_max_price`     int          NOT NULL DEFAULT 5000 COMMENT '自动退款金额上限（分）',
    `user_daily_apply_limit`    int          NOT NULL DEFAULT 3 COMMENT '用户当日售后申请次数阈值',
    `blacklist_user_ids`        varchar(2000) NOT NULL DEFAULT '[]' COMMENT '黑名单用户ID列表(JSON数组)',
    `suspicious_order_keywords` varchar(2000) NOT NULL DEFAULT '[]' COMMENT '可疑订单关键字列表(JSON数组)',
    `rule_version`              varchar(32)  NOT NULL DEFAULT 'v1' COMMENT '规则版本',
    `remark`                    varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
    `creator`                   varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`               datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`                   varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`               datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`                   bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_enabled_id` (`enabled`, `id`)
) COMMENT ='售后退款风控规则表';

INSERT INTO `trade_after_sale_refund_rule`
(`enabled`, `auto_refund_max_price`, `user_daily_apply_limit`, `blacklist_user_ids`,
 `suspicious_order_keywords`, `rule_version`, `remark`)
SELECT b'1', 5000, 3, '[]', '["TEST","MOCK"]', 'v1', 'HXY 默认退款分层规则'
WHERE NOT EXISTS (SELECT 1 FROM `trade_after_sale_refund_rule` WHERE `deleted` = b'0');

SET FOREIGN_KEY_CHECKS = 1;
