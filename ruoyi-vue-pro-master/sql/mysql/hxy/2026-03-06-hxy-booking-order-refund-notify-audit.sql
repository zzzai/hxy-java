SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY Booking 退款回调审计增强
-- 日期：2026-03-06
-- 目标：
-- 1) 预约订单记录支付退款单号
-- 2) 预约订单记录退款完成时间
-- =============================================

ALTER TABLE `booking_order`
    ADD COLUMN IF NOT EXISTS `pay_refund_id` BIGINT DEFAULT NULL COMMENT '退款单编号' AFTER `pay_order_id`,
    ADD COLUMN IF NOT EXISTS `refund_time` DATETIME DEFAULT NULL COMMENT '退款时间' AFTER `pay_time`;

CREATE INDEX IF NOT EXISTS `idx_booking_order_pay_refund_id` ON `booking_order` (`pay_refund_id`);

SET FOREIGN_KEY_CHECKS = 1;

