SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 技师佣金通知出站审计字段补齐
-- 日期：2026-03-02
-- 目标：
-- 1) 统一记录出站最近动作（创建/发送成功/发送失败/人工重试）
-- 2) 支持管理端按动作编码与动作业务号筛选
-- =============================================

ALTER TABLE `technician_commission_settlement_notify_outbox`
    ADD COLUMN IF NOT EXISTS `last_action_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '最近审计动作编码' AFTER `last_error_msg`,
    ADD COLUMN IF NOT EXISTS `last_action_biz_no` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '最近审计业务号' AFTER `last_action_code`,
    ADD COLUMN IF NOT EXISTS `last_action_time` DATETIME DEFAULT NULL COMMENT '最近审计动作时间' AFTER `last_action_biz_no`;

ALTER TABLE `technician_commission_settlement_notify_outbox`
    ADD INDEX `idx_notify_last_action_code` (`last_action_code`),
    ADD INDEX `idx_notify_last_action_biz_no` (`last_action_biz_no`);

SET FOREIGN_KEY_CHECKS = 1;
