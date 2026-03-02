SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 退款人工复核工单表脚本
-- 日期：2026-02-23
-- 目的：
-- 1) 人工复核路径工单化，形成可检索的总部工单池
-- 2) 支持规则命中/自动退款失败的 SLA 与升级对象跟踪
-- =============================================

CREATE TABLE IF NOT EXISTS `trade_after_sale_review_ticket`
(
    `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `after_sale_id`      bigint       NOT NULL COMMENT '售后单 ID',
    `order_id`           bigint       NOT NULL COMMENT '订单 ID',
    `order_item_id`      bigint       NOT NULL COMMENT '订单项 ID',
    `user_id`            bigint       NOT NULL COMMENT '用户 ID',
    `rule_code`          varchar(64)  NOT NULL DEFAULT '' COMMENT '命中规则编码',
    `decision_reason`    varchar(500) NOT NULL DEFAULT '' COMMENT '命中原因',
    `severity`           varchar(16)  NOT NULL DEFAULT 'P1' COMMENT '严重级别',
    `escalate_to`        varchar(64)  NOT NULL DEFAULT '' COMMENT '升级对象',
    `sla_deadline_time`  datetime              DEFAULT NULL COMMENT 'SLA 截止时间',
    `status`             tinyint      NOT NULL DEFAULT 0 COMMENT '0 待处理 10 已收口',
    `first_trigger_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次触发时间',
    `last_trigger_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近触发时间',
    `trigger_count`      int          NOT NULL DEFAULT 1 COMMENT '触发次数',
    `resolved_time`      datetime              DEFAULT NULL COMMENT '收口时间',
    `resolver_id`        bigint                DEFAULT NULL COMMENT '收口人 ID',
    `resolver_type`      int                   DEFAULT NULL COMMENT '收口人类型',
    `remark`             varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
    `creator`            varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`            varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`            bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_after_sale_id` (`after_sale_id`),
    KEY `idx_status_deadline` (`status`, `sla_deadline_time`),
    KEY `idx_severity_status` (`severity`, `status`)
) COMMENT ='售后人工复核工单表';

SET FOREIGN_KEY_CHECKS = 1;
