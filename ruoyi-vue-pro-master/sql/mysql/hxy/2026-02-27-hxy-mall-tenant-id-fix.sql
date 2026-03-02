-- HXY: Mall schema tenant isolation fix
-- Date: 2026-02-27
-- Purpose:
--   1) Backfill missing tenant_id column for legacy mall tables
--   2) Add tenant_id index for query performance
--   3) Normalize existing rows to tenant_id = 1 in single-tenant local env

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS hxy_fix_mall_tenant_column;
DELIMITER $$
CREATE PROCEDURE hxy_fix_mall_tenant_column(IN p_table VARCHAR(128))
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = 'tenant_id'
    ) THEN
        SET @sql_add_col = CONCAT(
            'ALTER TABLE `', p_table,
            '` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT ''租户编号'''
        );
        PREPARE stmt_add_col FROM @sql_add_col;
        EXECUTE stmt_add_col;
        DEALLOCATE PREPARE stmt_add_col;
    END IF;

    SET @idx_name = CONCAT('idx_', p_table, '_tenant_id');
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = @idx_name
    ) THEN
        SET @sql_add_idx = CONCAT(
            'CREATE INDEX `', @idx_name, '` ON `', p_table, '` (`tenant_id`)'
        );
        PREPARE stmt_add_idx FROM @sql_add_idx;
        EXECUTE stmt_add_idx;
        DEALLOCATE PREPARE stmt_add_idx;
    END IF;

    -- Single-tenant local normalization: keep legacy rows visible under tenant 1.
    SET @sql_update_tenant = CONCAT(
        'UPDATE `', p_table, '` SET tenant_id = 1 WHERE tenant_id = 0'
    );
    PREPARE stmt_update_tenant FROM @sql_update_tenant;
    EXECUTE stmt_update_tenant;
    DEALLOCATE PREPARE stmt_update_tenant;
END$$
DELIMITER ;

CALL hxy_fix_mall_tenant_column('promotion_coupon');
CALL hxy_fix_mall_tenant_column('promotion_coupon_template');
CALL hxy_fix_mall_tenant_column('promotion_discount_activity');
CALL hxy_fix_mall_tenant_column('promotion_reward_activity');
CALL hxy_fix_mall_tenant_column('trade_after_sale');
CALL hxy_fix_mall_tenant_column('trade_after_sale_log');
CALL hxy_fix_mall_tenant_column('trade_after_sale_refund_rule');
CALL hxy_fix_mall_tenant_column('trade_after_sale_review_ticket');
CALL hxy_fix_mall_tenant_column('trade_delivery_express');
CALL hxy_fix_mall_tenant_column('trade_order');
CALL hxy_fix_mall_tenant_column('trade_order_item');
CALL hxy_fix_mall_tenant_column('trade_service_order');

DROP PROCEDURE IF EXISTS hxy_fix_mall_tenant_column;
SET FOREIGN_KEY_CHECKS = 1;

-- Quick verification
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'promotion_coupon', 'promotion_coupon_template', 'promotion_discount_activity', 'promotion_reward_activity',
      'trade_after_sale', 'trade_after_sale_log', 'trade_after_sale_refund_rule', 'trade_after_sale_review_ticket',
      'trade_delivery_express', 'trade_order', 'trade_order_item', 'trade_service_order'
  )
  AND table_name NOT IN (
      SELECT table_name
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND column_name = 'tenant_id'
  );
