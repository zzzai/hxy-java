SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 门店生命周期批次台账守卫快照增强
-- 日期：2026-03-05
-- 目标：
-- 1) 增加守卫规则版本（guard_rule_version）
-- 2) 增加守卫配置快照（guard_config_snapshot_json）
-- =============================================

SET @ddl_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.columns
                          WHERE table_schema = DATABASE()
                            AND table_name = 'hxy_store_lifecycle_batch_log'
                            AND column_name = 'guard_rule_version'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_batch_log ADD COLUMN guard_rule_version VARCHAR(64) NOT NULL DEFAULT \"\" COMMENT \"守卫规则版本\" AFTER audit_summary'
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
                            AND table_name = 'hxy_store_lifecycle_batch_log'
                            AND column_name = 'guard_config_snapshot_json'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_batch_log ADD COLUMN guard_config_snapshot_json LONGTEXT COMMENT \"守卫配置快照 JSON\" AFTER guard_rule_version'
           )
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
