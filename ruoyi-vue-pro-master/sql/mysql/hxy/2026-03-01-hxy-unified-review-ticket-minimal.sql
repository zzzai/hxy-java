SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 统一工单中台（最小版）
-- 日期：2026-03-01
-- 目标：在现有售后复核工单上扩展服务履约/提成争议工单
-- =============================================

ALTER TABLE `trade_after_sale_review_ticket`
    ADD COLUMN IF NOT EXISTS `ticket_type` TINYINT NOT NULL DEFAULT 10 COMMENT '工单类型 10售后 20履约 30提成' AFTER `id`,
    ADD COLUMN IF NOT EXISTS `source_biz_no` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '来源业务单号' AFTER `after_sale_id`;

ALTER TABLE `trade_after_sale_review_ticket`
    MODIFY COLUMN `after_sale_id` BIGINT NULL COMMENT '售后单ID（售后工单必填）';

ALTER TABLE `trade_after_sale_review_ticket`
    ADD INDEX `idx_ticket_type_status` (`ticket_type`, `status`),
    ADD INDEX `idx_source_biz_no` (`source_biz_no`);

SET FOREIGN_KEY_CHECKS = 1;
