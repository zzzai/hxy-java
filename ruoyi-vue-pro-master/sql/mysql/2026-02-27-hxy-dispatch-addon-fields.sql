-- 预约订单：新增派单模式和加钟字段
ALTER TABLE booking_order ADD COLUMN dispatch_mode TINYINT DEFAULT 1 COMMENT '派单模式 1=点钟 2=排钟';
ALTER TABLE booking_order ADD COLUMN parent_order_id BIGINT DEFAULT NULL COMMENT '父订单ID（加钟/升级关联）';
ALTER TABLE booking_order ADD COLUMN is_addon TINYINT DEFAULT 0 COMMENT '是否加钟子订单 0=否 1=是';
ALTER TABLE booking_order ADD COLUMN addon_type TINYINT DEFAULT NULL COMMENT '加钟类型 1=加钟 2=升级 3=加项目';
CREATE INDEX idx_booking_order_parent ON booking_order(parent_order_id);
