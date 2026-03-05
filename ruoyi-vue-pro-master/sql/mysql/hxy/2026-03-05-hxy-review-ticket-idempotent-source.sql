SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 统一工单来源幂等键收口
-- 日期：2026-03-05
-- 目标：
-- 1) 按 ticket_type + source_biz_no + deleted 建唯一键，支撑跨模块幂等 upsert
-- 2) 历史空来源号补齐，避免唯一键上线失败
-- =============================================

-- 历史数据回填：优先沿用 after_sale_id，再回退 ticket id
UPDATE `trade_after_sale_review_ticket`
SET `source_biz_no` = CONCAT('AFTER_SALE#', `after_sale_id`)
WHERE (`source_biz_no` IS NULL OR `source_biz_no` = '')
  AND `after_sale_id` IS NOT NULL;

UPDATE `trade_after_sale_review_ticket`
SET `source_biz_no` = CONCAT('TICKET#', `id`)
WHERE (`source_biz_no` IS NULL OR `source_biz_no` = '');

-- 历史重复键去重：保留最早一条，其余追加 #id 后缀
UPDATE `trade_after_sale_review_ticket` t
    JOIN (
    SELECT `ticket_type`, `source_biz_no`, MIN(`id`) AS keep_id
    FROM `trade_after_sale_review_ticket`
    WHERE `deleted` = b'0'
    GROUP BY `ticket_type`, `source_biz_no`
    HAVING COUNT(1) > 1
) dup
ON t.`ticket_type` = dup.`ticket_type`
    AND t.`source_biz_no` = dup.`source_biz_no`
SET t.`source_biz_no` = CONCAT(LEFT(t.`source_biz_no`, 52), '#', t.`id`)
WHERE t.`deleted` = b'0'
  AND t.`id` <> dup.keep_id;

SET @idx_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE table_schema = DATABASE()
                            AND table_name = 'trade_after_sale_review_ticket'
                            AND index_name = 'uk_ticket_type_source_biz_no'),
                   'SELECT 1',
                   'ALTER TABLE trade_after_sale_review_ticket ADD UNIQUE INDEX uk_ticket_type_source_biz_no (ticket_type, source_biz_no, deleted)'
           )
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
