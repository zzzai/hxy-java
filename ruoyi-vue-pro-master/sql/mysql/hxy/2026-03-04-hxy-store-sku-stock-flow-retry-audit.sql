SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- HXY: 门店 SKU 库存流水补充批量重试审计字段

ALTER TABLE `hxy_store_product_sku_stock_flow`
    ADD COLUMN IF NOT EXISTS `last_retry_operator` varchar(64) NOT NULL DEFAULT '' COMMENT '最近重试操作人' AFTER `execute_time`,
    ADD COLUMN IF NOT EXISTS `last_retry_source`   varchar(32) NOT NULL DEFAULT '' COMMENT '最近重试来源' AFTER `last_retry_operator`;

SET FOREIGN_KEY_CHECKS = 1;
