SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 技师提成冲正幂等键补强（origin_commission_id）
-- 日期：2026-03-02
-- 目标：
-- 1) 冲正记录显式关联原佣金记录
-- 2) 通过唯一索引防止同原佣金重复冲正
-- =============================================

ALTER TABLE `technician_commission`
    ADD COLUMN IF NOT EXISTS `origin_commission_id` BIGINT DEFAULT NULL COMMENT '原佣金ID（冲正幂等键）' AFTER `staff_id`;

ALTER TABLE `technician_commission`
    ADD UNIQUE INDEX `uk_origin_commission_id` (`origin_commission_id`);

SET FOREIGN_KEY_CHECKS = 1;
