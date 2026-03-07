SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 技师提成链路追溯字段补齐（计提/冲正）
-- 日期：2026-03-08
-- 目标：
-- 1) 提成记录可追溯到 orderItem/serviceOrder/sourceBizNo
-- 2) 支撑计提幂等命中与四账审计聚合
-- =============================================

ALTER TABLE `technician_commission`
    ADD COLUMN IF NOT EXISTS `order_item_id` BIGINT DEFAULT NULL COMMENT '交易订单项ID' AFTER `order_id`,
    ADD COLUMN IF NOT EXISTS `service_order_id` BIGINT DEFAULT NULL COMMENT '服务履约单ID' AFTER `order_item_id`,
    ADD COLUMN IF NOT EXISTS `source_biz_no` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '追溯业务号' AFTER `biz_no`;

ALTER TABLE `technician_commission`
    ADD INDEX `idx_technician_commission_order_item_id` (`order_item_id`),
    ADD INDEX `idx_technician_commission_service_order_id` (`service_order_id`),
    ADD INDEX `idx_technician_commission_source_biz_no` (`source_biz_no`);

SET FOREIGN_KEY_CHECKS = 1;
