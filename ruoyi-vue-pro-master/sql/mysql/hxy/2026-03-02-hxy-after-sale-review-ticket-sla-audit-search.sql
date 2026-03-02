SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 售后统一工单：SLA 维度可检索 + 审计字段对齐
ALTER TABLE `trade_after_sale_review_ticket`
    ADD COLUMN IF NOT EXISTS `last_action_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '最近审计动作编码' AFTER `resolve_biz_no`,
    ADD COLUMN IF NOT EXISTS `last_action_biz_no` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '最近审计业务号' AFTER `last_action_code`,
    ADD COLUMN IF NOT EXISTS `last_action_time` DATETIME DEFAULT NULL COMMENT '最近审计动作时间' AFTER `last_action_biz_no`;

ALTER TABLE `trade_after_sale_review_ticket`
    ADD INDEX `idx_last_action_code` (`last_action_code`),
    ADD INDEX `idx_last_action_biz_no` (`last_action_biz_no`);

SET FOREIGN_KEY_CHECKS = 1;
