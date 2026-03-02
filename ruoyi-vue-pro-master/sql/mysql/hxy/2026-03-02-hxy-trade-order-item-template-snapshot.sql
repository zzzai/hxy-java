-- HXY 交易订单项补充模板/价格来源快照字段（增量）
-- 执行前请确认数据库版本支持 IF NOT EXISTS（MySQL 8.0+）

ALTER TABLE `trade_order_item`
    ADD COLUMN IF NOT EXISTS `template_version_id` bigint NULL COMMENT '模板版本ID' AFTER `addon_snapshot_json`,
    ADD COLUMN IF NOT EXISTS `template_snapshot_json` varchar(4000) NULL COMMENT '模板快照JSON' AFTER `template_version_id`,
    ADD COLUMN IF NOT EXISTS `price_source_snapshot_json` varchar(2000) NULL COMMENT '价格来源快照JSON' AFTER `template_snapshot_json`;
