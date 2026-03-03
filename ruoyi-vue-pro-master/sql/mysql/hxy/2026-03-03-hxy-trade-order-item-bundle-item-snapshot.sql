-- HXY 交易订单项补充套餐子项快照字段（增量）
-- 执行前请确认数据库版本支持 IF NOT EXISTS（MySQL 8.0+）

ALTER TABLE `trade_order_item`
    ADD COLUMN IF NOT EXISTS `bundle_item_snapshot_json` varchar(2000) NULL COMMENT '套餐子项快照JSON' AFTER `price_source_snapshot_json`;
