SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY Booking 退款重放工单同步审计台账
-- 日期：2026-03-07
-- 目标：
-- 1) 固化 runId+notifyLogId 工单同步轨迹
-- 2) 支撑同步幂等跳过、失败追溯与详情查询
-- =============================================

CREATE TABLE IF NOT EXISTS `hxy_booking_refund_replay_ticket_sync_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `run_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '批次号',
    `notify_log_id` BIGINT NOT NULL COMMENT '退款回调台账ID',
    `source_biz_no` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '工单来源号',
    `ticket_id` BIGINT DEFAULT NULL COMMENT '工单ID',
    `status` VARCHAR(16) NOT NULL DEFAULT '' COMMENT '同步状态(SUCCESS/SKIP/FAIL)',
    `error_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '错误码',
    `error_msg` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '错误信息',
    `operator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '操作人',
    `sync_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_hxy_brrtsl_run_notify_id` (`run_id`, `notify_log_id`, `id`),
    KEY `idx_hxy_brrtsl_status` (`status`),
    KEY `idx_hxy_brrtsl_sync_time` (`sync_time`)
) COMMENT='booking退款重放工单同步审计台账';

SET FOREIGN_KEY_CHECKS = 1;
