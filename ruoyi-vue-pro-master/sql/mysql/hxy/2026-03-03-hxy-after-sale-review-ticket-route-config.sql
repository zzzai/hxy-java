SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 售后统一工单：SLA 路由规则配置表
CREATE TABLE IF NOT EXISTS `trade_after_sale_review_ticket_route`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `scope`       varchar(32)  NOT NULL DEFAULT '' COMMENT '作用域：RULE/TYPE_SEVERITY/TYPE_DEFAULT/GLOBAL_DEFAULT',
    `rule_code`   varchar(64)  NOT NULL DEFAULT '' COMMENT '规则编码（RULE作用域使用）',
    `ticket_type` int          NOT NULL DEFAULT 0 COMMENT '工单类型（TYPE_SEVERITY/TYPE_DEFAULT作用域使用）',
    `severity`    varchar(16)  NOT NULL DEFAULT '' COMMENT '严重级别（TYPE_SEVERITY作用域使用）',
    `escalate_to` varchar(64)  NOT NULL DEFAULT '' COMMENT '升级对象',
    `sla_minutes` int          NOT NULL DEFAULT 120 COMMENT 'SLA分钟',
    `enabled`     bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序（越小优先级越高）',
    `remark`      varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
    `creator`     varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scope_route_key` (`scope`, `rule_code`, `ticket_type`, `severity`),
    KEY `idx_enabled_scope_sort` (`enabled`, `scope`, `sort`, `id`)
) COMMENT ='售后复核工单路由规则表';

-- 初始化默认规则（与旧硬编码行为保持一致）
INSERT INTO `trade_after_sale_review_ticket_route`
(`scope`, `rule_code`, `ticket_type`, `severity`, `escalate_to`, `sla_minutes`, `enabled`, `sort`, `remark`)
VALUES
    ('RULE', 'BLACKLIST_USER', 0, 'P0', 'HQ_RISK_FINANCE', 30, b'1', 10, '默认规则：黑名单'),
    ('RULE', 'SUSPICIOUS_ORDER', 0, 'P0', 'HQ_RISK_FINANCE', 30, b'1', 11, '默认规则：可疑订单'),
    ('RULE', 'AMOUNT_OVER_LIMIT', 0, 'P1', 'HQ_FINANCE', 120, b'1', 12, '默认规则：金额超限'),
    ('RULE', 'HIGH_FREQUENCY', 0, 'P1', 'HQ_AFTER_SALE', 120, b'1', 13, '默认规则：高频退款'),
    ('RULE', 'AUTO_REFUND_EXECUTE_FAIL', 0, 'P0', 'PAY_DEVOPS', 15, b'1', 14, '默认规则：自动退款失败'),
    ('RULE', 'REFUND_LIMIT_CHANGED', 0, 'P1', 'HQ_AFTER_SALE', 120, b'1', 15, '默认规则：退款上限收紧'),
    ('TYPE_DEFAULT', '', 10, 'P1', 'HQ_AFTER_SALE', 120, b'1', 20, '售后工单类型默认'),
    ('TYPE_DEFAULT', '', 20, 'P1', 'HQ_SERVICE_OPS', 90, b'1', 21, '服务履约工单类型默认'),
    ('TYPE_DEFAULT', '', 30, 'P1', 'HQ_FINANCE', 120, b'1', 22, '提成争议工单类型默认'),
    ('TYPE_SEVERITY', '', 10, 'P0', 'HQ_RISK_FINANCE', 30, b'1', 30, '售后P0默认'),
    ('TYPE_SEVERITY', '', 10, 'P1', 'HQ_AFTER_SALE', 120, b'1', 31, '售后P1默认'),
    ('TYPE_SEVERITY', '', 20, 'P0', 'HQ_SERVICE_OPS', 30, b'1', 32, '履约P0默认'),
    ('TYPE_SEVERITY', '', 20, 'P1', 'HQ_SERVICE_OPS', 90, b'1', 33, '履约P1默认'),
    ('TYPE_SEVERITY', '', 30, 'P0', 'HQ_FINANCE', 30, b'1', 34, '提成P0默认'),
    ('TYPE_SEVERITY', '', 30, 'P1', 'HQ_FINANCE', 120, b'1', 35, '提成P1默认'),
    ('GLOBAL_DEFAULT', '', 0, 'P1', 'HQ_AFTER_SALE', 120, b'1', 100, '全局兜底默认')
ON DUPLICATE KEY UPDATE
    `escalate_to` = VALUES(`escalate_to`),
    `sla_minutes` = VALUES(`sla_minutes`),
    `enabled` = VALUES(`enabled`),
    `sort` = VALUES(`sort`),
    `remark` = VALUES(`remark`),
    `updater` = 'system';

-- 规范历史数据：TYPE_DEFAULT / GLOBAL_DEFAULT 固定使用 P1，避免和运行时匹配键不一致
UPDATE `trade_after_sale_review_ticket_route`
SET `severity` = 'P1',
    `updater` = 'system'
WHERE `scope` IN ('TYPE_DEFAULT', 'GLOBAL_DEFAULT')
  AND (`severity` IS NULL OR `severity` <> 'P1');

SET FOREIGN_KEY_CHECKS = 1;
