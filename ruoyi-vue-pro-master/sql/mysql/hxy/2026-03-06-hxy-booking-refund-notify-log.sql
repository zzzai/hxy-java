SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY Booking 退款回调失败台账
-- 日期：2026-03-06
-- 目标：
-- 1) 记录退款回调成功/失败全量轨迹
-- 2) 支持失败重放与重试调度审计
-- =============================================

CREATE TABLE IF NOT EXISTS `hxy_booking_refund_notify_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` BIGINT DEFAULT NULL COMMENT '预约订单ID',
    `merchant_refund_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '商户退款单号',
    `pay_refund_id` BIGINT DEFAULT NULL COMMENT '支付退款单ID',
    `status` VARCHAR(16) NOT NULL DEFAULT 'fail' COMMENT '处理状态(success|fail)',
    `error_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '错误码',
    `error_msg` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '错误信息',
    `raw_payload` LONGTEXT DEFAULT NULL COMMENT '原始回调载荷',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重放重试次数',
    `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_hxy_brnl_order_id` (`order_id`),
    KEY `idx_hxy_brnl_pay_refund_id` (`pay_refund_id`),
    KEY `idx_hxy_brnl_merchant_refund_id` (`merchant_refund_id`),
    KEY `idx_hxy_brnl_status_next_retry` (`status`, `next_retry_time`),
    KEY `idx_hxy_brnl_create_time` (`create_time`)
) COMMENT='booking退款回调台账';

SET FOREIGN_KEY_CHECKS = 1;
