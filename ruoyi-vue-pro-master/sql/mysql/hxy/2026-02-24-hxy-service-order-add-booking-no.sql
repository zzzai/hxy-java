SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 服务履约单新增 booking_no 字段（增量）
-- 日期：2026-02-24
-- 目的：
-- 1) 服务履约单独立持久化预约单号，避免仅写入 remark
-- =============================================

ALTER TABLE `trade_service_order`
    ADD COLUMN IF NOT EXISTS `booking_no` varchar(64) NOT NULL DEFAULT '' COMMENT '预约单号' AFTER `source`;

SET FOREIGN_KEY_CHECKS = 1;
