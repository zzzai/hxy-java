SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 门店生命周期变更单 SLA 与审计增强
-- 日期：2026-03-05
-- 目标：
-- 1) 支持提交时间、SLA 截止时间
-- 2) 支持最后动作审计维度筛选
-- =============================================

SET @ddl_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.columns
                          WHERE table_schema = DATABASE()
                            AND table_name = 'hxy_store_lifecycle_change_order'
                            AND column_name = 'submit_time'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_change_order ADD COLUMN submit_time DATETIME NULL DEFAULT NULL COMMENT ''提交时间'' AFTER approve_time'
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
                            AND table_name = 'hxy_store_lifecycle_change_order'
                            AND column_name = 'sla_deadline_time'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_change_order ADD COLUMN sla_deadline_time DATETIME NULL DEFAULT NULL COMMENT ''SLA截止时间'' AFTER submit_time'
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
                            AND table_name = 'hxy_store_lifecycle_change_order'
                            AND column_name = 'last_action_code'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_change_order ADD COLUMN last_action_code VARCHAR(32) NOT NULL DEFAULT '''' COMMENT ''最后动作编码'' AFTER sla_deadline_time'
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
                            AND table_name = 'hxy_store_lifecycle_change_order'
                            AND column_name = 'last_action_operator'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_change_order ADD COLUMN last_action_operator VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''最后动作操作人'' AFTER last_action_code'
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
                            AND table_name = 'hxy_store_lifecycle_change_order'
                            AND column_name = 'last_action_time'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_change_order ADD COLUMN last_action_time DATETIME NULL DEFAULT NULL COMMENT ''最后动作时间'' AFTER last_action_operator'
           )
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE table_schema = DATABASE()
                            AND table_name = 'hxy_store_lifecycle_change_order'
                            AND index_name = 'idx_status_sla_deadline'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_change_order ADD INDEX idx_status_sla_deadline (status, sla_deadline_time)'
           )
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE table_schema = DATABASE()
                            AND table_name = 'hxy_store_lifecycle_change_order'
                            AND index_name = 'idx_last_action_time'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_change_order ADD INDEX idx_last_action_time (last_action_time)'
           )
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
