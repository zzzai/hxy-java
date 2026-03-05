SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 门店生命周期守卫复核台账
-- 日期：2026-03-05
-- 目标：
-- 1) 持久化 /product/store/lifecycle-guard/recheck-by-batch/execute 复核结果
-- 2) 支持按复核编号/批次/状态/操作人/来源/时间分页检索
-- =============================================

CREATE TABLE IF NOT EXISTS `hxy_store_lifecycle_recheck_log`
(
    `id`                         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `recheck_no`                 VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '复核编号',
    `log_id`                     BIGINT       NOT NULL DEFAULT 0 COMMENT '批次台账ID',
    `batch_no`                   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '批次号',
    `target_lifecycle_status`    INT          NOT NULL DEFAULT 10 COMMENT '目标生命周期状态',
    `total_count`                INT          NOT NULL DEFAULT 0 COMMENT '总门店数',
    `blocked_count`              INT          NOT NULL DEFAULT 0 COMMENT '阻塞数',
    `warning_count`              INT          NOT NULL DEFAULT 0 COMMENT '告警数',
    `detail_json`                LONGTEXT              COMMENT '逐店复核明细 JSON',
    `detail_parse_error`         BIT(1)       NOT NULL DEFAULT b'0' COMMENT '明细是否解析失败',
    `guard_rule_version`         VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '守卫规则版本',
    `guard_config_snapshot_json` LONGTEXT              COMMENT '守卫配置快照 JSON',
    `operator`                   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '操作人',
    `source`                     VARCHAR(32)  NOT NULL DEFAULT 'ADMIN_UI' COMMENT '来源',
    `creator`                    VARCHAR(64)           DEFAULT '' COMMENT '创建者',
    `create_time`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`                    VARCHAR(64)           DEFAULT '' COMMENT '更新者',
    `update_time`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`                    BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='门店生命周期守卫复核台账';

SET @idx_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE table_schema = DATABASE()
                            AND table_name = 'hxy_store_lifecycle_recheck_log'
                            AND index_name = 'uk_recheck_no'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_recheck_log ADD UNIQUE INDEX uk_recheck_no (recheck_no)'
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
                            AND table_name = 'hxy_store_lifecycle_recheck_log'
                            AND index_name = 'idx_log_id'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_recheck_log ADD INDEX idx_log_id (log_id)'
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
                            AND table_name = 'hxy_store_lifecycle_recheck_log'
                            AND index_name = 'idx_batch_no'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_recheck_log ADD INDEX idx_batch_no (batch_no)'
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
                            AND table_name = 'hxy_store_lifecycle_recheck_log'
                            AND index_name = 'idx_target_lifecycle_status'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_recheck_log ADD INDEX idx_target_lifecycle_status (target_lifecycle_status)'
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
                            AND table_name = 'hxy_store_lifecycle_recheck_log'
                            AND index_name = 'idx_create_time'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_recheck_log ADD INDEX idx_create_time (create_time)'
           )
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
