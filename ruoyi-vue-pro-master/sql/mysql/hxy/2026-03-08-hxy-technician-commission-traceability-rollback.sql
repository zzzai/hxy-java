SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 技师提成链路追溯字段回滚脚本（幂等）
-- 日期：2026-03-08
-- 回滚对象：
-- 1) technician_commission 新增索引
-- 2) technician_commission 新增字段 order_item_id/service_order_id/source_biz_no
-- =============================================

SET @ddl_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE table_schema = DATABASE()
                            AND table_name = 'technician_commission'
                            AND index_name = 'idx_technician_commission_order_item_id'),
                   'ALTER TABLE technician_commission DROP INDEX idx_technician_commission_order_item_id',
                   'SELECT 1'
           )
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE table_schema = DATABASE()
                            AND table_name = 'technician_commission'
                            AND index_name = 'idx_technician_commission_service_order_id'),
                   'ALTER TABLE technician_commission DROP INDEX idx_technician_commission_service_order_id',
                   'SELECT 1'
           )
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE table_schema = DATABASE()
                            AND table_name = 'technician_commission'
                            AND index_name = 'idx_technician_commission_source_biz_no'),
                   'ALTER TABLE technician_commission DROP INDEX idx_technician_commission_source_biz_no',
                   'SELECT 1'
           )
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.columns
                          WHERE table_schema = DATABASE()
                            AND table_name = 'technician_commission'
                            AND column_name = 'order_item_id'),
                   'ALTER TABLE technician_commission DROP COLUMN order_item_id',
                   'SELECT 1'
           )
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.columns
                          WHERE table_schema = DATABASE()
                            AND table_name = 'technician_commission'
                            AND column_name = 'service_order_id'),
                   'ALTER TABLE technician_commission DROP COLUMN service_order_id',
                   'SELECT 1'
           )
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.columns
                          WHERE table_schema = DATABASE()
                            AND table_name = 'technician_commission'
                            AND column_name = 'source_biz_no'),
                   'ALTER TABLE technician_commission DROP COLUMN source_biz_no',
                   'SELECT 1'
           )
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
