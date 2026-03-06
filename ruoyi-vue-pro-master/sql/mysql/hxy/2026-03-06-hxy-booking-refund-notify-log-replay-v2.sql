SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY Booking 退款回调重放 V2 审计字段增强
-- 日期：2026-03-06
-- 目标：
-- 1) 记录最近重放执行人、时间、结果与备注
-- 2) 支撑批量重放 dry-run 与自动补偿任务审计追溯
-- =============================================

ALTER TABLE `hxy_booking_refund_notify_log`
    ADD COLUMN IF NOT EXISTS `last_replay_operator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '最近重放操作人' AFTER `next_retry_time`,
    ADD COLUMN IF NOT EXISTS `last_replay_time` DATETIME DEFAULT NULL COMMENT '最近重放时间' AFTER `last_replay_operator`,
    ADD COLUMN IF NOT EXISTS `last_replay_result` VARCHAR(16) NOT NULL DEFAULT '' COMMENT '最近重放结果(SUCCESS/SKIP/FAIL)' AFTER `last_replay_time`,
    ADD COLUMN IF NOT EXISTS `last_replay_remark` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '最近重放备注' AFTER `last_replay_result`;

CREATE INDEX IF NOT EXISTS `idx_hxy_brnl_replay_time` ON `hxy_booking_refund_notify_log` (`last_replay_time`);

SET FOREIGN_KEY_CHECKS = 1;
