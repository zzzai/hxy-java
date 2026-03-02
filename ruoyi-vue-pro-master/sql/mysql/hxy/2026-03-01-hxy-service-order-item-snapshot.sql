SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 服务商品加钟/加项目：并入交易订单项快照，保障结算可追溯
ALTER TABLE `trade_order_item`
    ADD COLUMN IF NOT EXISTS `addon_type` tinyint NULL COMMENT '服务加项类型 1=加钟 2=升级 3=加项目' AFTER `pic_url`,
    ADD COLUMN IF NOT EXISTS `addon_snapshot_json` text NULL COMMENT '服务加项快照（JSON）' AFTER `addon_type`;

ALTER TABLE `trade_service_order`
    ADD COLUMN IF NOT EXISTS `addon_type` tinyint NULL COMMENT '服务加项类型 1=加钟 2=升级 3=加项目' AFTER `sku_id`,
    ADD COLUMN IF NOT EXISTS `addon_snapshot_json` text NULL COMMENT '服务加项快照（JSON）' AFTER `addon_type`,
    ADD COLUMN IF NOT EXISTS `order_item_snapshot_json` text NULL COMMENT '订单项快照（JSON）' AFTER `addon_snapshot_json`;

SET FOREIGN_KEY_CHECKS = 1;
