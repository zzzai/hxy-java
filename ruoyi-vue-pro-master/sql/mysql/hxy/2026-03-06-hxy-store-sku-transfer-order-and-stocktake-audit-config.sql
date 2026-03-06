SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 门店跨店调拨单 + 盘点差异稽核配置
-- 日期：2026-03-06
-- =============================================

CREATE TABLE IF NOT EXISTS `hxy_store_sku_transfer_order`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`            VARCHAR(64)  NOT NULL COMMENT '调拨单号',
    `from_store_id`       BIGINT       NOT NULL COMMENT '源门店ID',
    `from_store_name`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '源门店名称',
    `to_store_id`         BIGINT       NOT NULL COMMENT '目标门店ID',
    `to_store_name`       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '目标门店名称',
    `reason`              VARCHAR(255) NOT NULL DEFAULT '' COMMENT '原因',
    `remark`              VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
    `status`              TINYINT      NOT NULL COMMENT '状态：0草稿 10待审 20已通过 30已驳回 40已取消',
    `detail_json`         TEXT         NOT NULL COMMENT '调拨明细JSON',
    `apply_operator`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '申请人',
    `apply_source`        VARCHAR(32)  NOT NULL DEFAULT 'ADMIN_UI' COMMENT '申请来源',
    `approve_operator`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '审批人',
    `approve_remark`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '审批备注',
    `approve_time`        DATETIME              DEFAULT NULL COMMENT '审批时间',
    `last_action_code`    VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '最后动作编码',
    `last_action_operator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '最后动作操作人',
    `last_action_time`    DATETIME              DEFAULT NULL COMMENT '最后动作时间',
    `creator`             VARCHAR(64)           DEFAULT '' COMMENT '创建者',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`             VARCHAR(64)           DEFAULT '' COMMENT '更新者',
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`           BIGINT       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='门店SKU跨店调拨单';

SET @ddl := IF(
        EXISTS(SELECT 1
               FROM information_schema.statistics
               WHERE table_schema = DATABASE()
                 AND table_name = 'hxy_store_sku_transfer_order'
                 AND index_name = 'uk_order_no'),
        'SELECT 1',
        'ALTER TABLE hxy_store_sku_transfer_order ADD UNIQUE INDEX uk_order_no (order_no, deleted)'
            );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
        EXISTS(SELECT 1
               FROM information_schema.statistics
               WHERE table_schema = DATABASE()
                 AND table_name = 'hxy_store_sku_transfer_order'
                 AND index_name = 'idx_from_store_status_time'),
        'SELECT 1',
        'ALTER TABLE hxy_store_sku_transfer_order ADD INDEX idx_from_store_status_time (from_store_id, status, create_time)'
            );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
        EXISTS(SELECT 1
               FROM information_schema.statistics
               WHERE table_schema = DATABASE()
                 AND table_name = 'hxy_store_sku_transfer_order'
                 AND index_name = 'idx_to_store_status_time'),
        'SELECT 1',
        'ALTER TABLE hxy_store_sku_transfer_order ADD INDEX idx_to_store_status_time (to_store_id, status, create_time)'
            );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
        EXISTS(SELECT 1
               FROM information_schema.statistics
               WHERE table_schema = DATABASE()
                 AND table_name = 'hxy_store_sku_transfer_order'
                 AND index_name = 'idx_last_action_time'),
        'SELECT 1',
        'ALTER TABLE hxy_store_sku_transfer_order ADD INDEX idx_last_action_time (last_action_code, last_action_time)'
            );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO infra_config (`category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `updater`, `deleted`)
SELECT 'mall.product.stocktake.audit', 2, seed.name, seed.config_key, seed.value, b'0', seed.remark, '1', '1', b'0'
FROM (
         SELECT '盘点差异稽核工单开关' AS name,
                'product.stocktake.audit-ticket.enabled' AS config_key,
                'true' AS value,
                '盘点审批通过后，若差异超过阈值则自动联动统一工单（fail-open）' AS remark
         UNION ALL
         SELECT '盘点差异稽核阈值',
                'product.stocktake.audit-threshold',
                '20',
                '盘点差异累计绝对值阈值（分项绝对值求和），超阈值联动统一工单'
     ) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM infra_config c
    WHERE c.config_key = seed.config_key
      AND c.deleted = 0
);

SET FOREIGN_KEY_CHECKS = 1;
