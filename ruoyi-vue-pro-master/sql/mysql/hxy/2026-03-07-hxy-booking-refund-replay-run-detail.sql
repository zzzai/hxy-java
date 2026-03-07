SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY Booking 退款回调重放批次明细台账
-- 日期：2026-03-07
-- 目标：
-- 1) 固化 runId 逐条重放结果，支持运营追溯
-- 2) 支撑 runId 级汇总看板与失败明细工单联动
-- =============================================

CREATE TABLE IF NOT EXISTS `hxy_booking_refund_replay_run_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `run_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '批次号',
    `run_log_id` BIGINT DEFAULT NULL COMMENT '批次台账ID',
    `notify_log_id` BIGINT NOT NULL COMMENT '退款回调台账ID',
    `order_id` BIGINT DEFAULT NULL COMMENT '预约订单ID',
    `pay_refund_id` BIGINT DEFAULT NULL COMMENT '支付退款单ID',
    `result_status` VARCHAR(16) NOT NULL DEFAULT '' COMMENT '重放结果(SUCCESS/SKIP/FAIL)',
    `result_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '结果码',
    `result_msg` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '结果消息',
    `warning_tag` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '告警标签(如 FOUR_ACCOUNT_REFRESH_WARN)',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hxy_brrd_run_notify` (`run_id`, `notify_log_id`),
    KEY `idx_hxy_brrd_run_id` (`run_id`),
    KEY `idx_hxy_brrd_run_log_id` (`run_log_id`),
    KEY `idx_hxy_brrd_result_status` (`result_status`)
) COMMENT='booking退款回调重放批次明细台账';

SET FOREIGN_KEY_CHECKS = 1;
