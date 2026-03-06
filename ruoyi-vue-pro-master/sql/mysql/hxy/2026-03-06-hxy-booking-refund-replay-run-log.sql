SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY Booking 退款回调重放批次运行台账
-- 日期：2026-03-06
-- 目标：
-- 1) 支撑手工/任务触发的重放批次审计
-- 2) 固化 runId 与成功/跳过/失败统计
-- =============================================

CREATE TABLE IF NOT EXISTS `hxy_booking_refund_replay_run_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `run_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '批次号',
    `trigger_source` VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT '触发来源(MANUAL/JOB)',
    `operator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '操作人',
    `dry_run` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否预演',
    `limit_size` INT NOT NULL DEFAULT 0 COMMENT '扫描上限',
    `scanned_count` INT NOT NULL DEFAULT 0 COMMENT '扫描总数',
    `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功数',
    `skip_count` INT NOT NULL DEFAULT 0 COMMENT '跳过数',
    `fail_count` INT NOT NULL DEFAULT 0 COMMENT '失败数',
    `status` VARCHAR(16) NOT NULL DEFAULT 'started' COMMENT '批次状态(started/success/partial_fail/fail)',
    `error_msg` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '错误/告警摘要',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hxy_brrl_run_id` (`run_id`),
    KEY `idx_hxy_brrl_status_start_time` (`status`, `start_time`),
    KEY `idx_hxy_brrl_trigger_start_time` (`trigger_source`, `start_time`),
    KEY `idx_hxy_brrl_operator` (`operator`)
) COMMENT='booking退款回调重放批次运行台账';

SET FOREIGN_KEY_CHECKS = 1;
