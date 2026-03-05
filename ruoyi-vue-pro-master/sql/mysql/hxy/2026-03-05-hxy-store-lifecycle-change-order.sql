SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 门店生命周期变更单（审批化最小闭环）
-- 日期：2026-03-05
-- 目标：
-- 1) 生命周期变更由“直接改状态”升级为“变更单提交+审批”
-- 2) 提交/审批过程固化守卫快照，支持审计追溯
-- =============================================

CREATE TABLE IF NOT EXISTS `hxy_store_lifecycle_change_order`
(
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`              VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '变更单号',
    `store_id`              BIGINT       NOT NULL DEFAULT 0 COMMENT '门店ID',
    `store_name`            VARCHAR(128) NOT NULL DEFAULT '' COMMENT '门店名称',
    `from_lifecycle_status` INT          NOT NULL DEFAULT 10 COMMENT '变更前生命周期状态',
    `to_lifecycle_status`   INT          NOT NULL DEFAULT 10 COMMENT '目标生命周期状态',
    `reason`                VARCHAR(512) NOT NULL DEFAULT '' COMMENT '变更原因',
    `apply_operator`        VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '申请人',
    `apply_source`          VARCHAR(32)  NOT NULL DEFAULT 'ADMIN_UI' COMMENT '申请来源',
    `status`                INT          NOT NULL DEFAULT 0 COMMENT '状态:0草稿 10待审批 20已通过 30已驳回 40已取消',
    `guard_snapshot_json`   LONGTEXT              COMMENT '守卫快照JSON',
    `guard_blocked`         BIT(1)       NOT NULL DEFAULT b'0' COMMENT '守卫是否阻塞',
    `guard_warnings`        VARCHAR(1024)         DEFAULT '' COMMENT '守卫告警摘要',
    `approve_operator`      VARCHAR(64)           DEFAULT '' COMMENT '审批/处理人',
    `approve_remark`        VARCHAR(512)          DEFAULT '' COMMENT '审批/处理备注',
    `approve_time`          DATETIME              DEFAULT NULL COMMENT '审批/处理时间',
    `creator`               VARCHAR(64)           DEFAULT '' COMMENT '创建者',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`               VARCHAR(64)           DEFAULT '' COMMENT '更新者',
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='门店生命周期变更单';

SET @idx_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE table_schema = DATABASE()
                            AND table_name = 'hxy_store_lifecycle_change_order'
                            AND index_name = 'uk_order_no'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_change_order ADD UNIQUE INDEX uk_order_no (order_no)'
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
                            AND index_name = 'idx_store_id'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_change_order ADD INDEX idx_store_id (store_id)'
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
                            AND index_name = 'idx_status'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_change_order ADD INDEX idx_status (status)'
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
                            AND index_name = 'idx_from_to_status'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_change_order ADD INDEX idx_from_to_status (from_lifecycle_status, to_lifecycle_status)'
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
                            AND index_name = 'idx_create_time'),
                   'SELECT 1',
                   'ALTER TABLE hxy_store_lifecycle_change_order ADD INDEX idx_create_time (create_time)'
           )
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
