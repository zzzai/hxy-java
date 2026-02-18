-- 支付/履约补丁：预约核销会员卡流水表
-- 日期：2026-02-15
-- 说明：支持预约订单核销时对会员卡扣减留痕

CREATE TABLE IF NOT EXISTS `eb_member_card_usage` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_card_id` bigint(20) NOT NULL COMMENT '会员卡ID（eb_member_card.id）',
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `order_id` bigint(20) NOT NULL COMMENT '预约订单ID（eb_booking_order.id）',
  `usage_type` tinyint(1) NOT NULL COMMENT '使用类型：1=次数 2=金额',
  `used_times` int(11) NOT NULL DEFAULT 0 COMMENT '本次使用次数',
  `used_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '本次使用金额',
  `before_times` int(11) NOT NULL DEFAULT 0 COMMENT '使用前次数（兼容字段）',
  `after_times` int(11) NOT NULL DEFAULT 0 COMMENT '使用后次数（兼容字段）',
  `before_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '使用前余额',
  `after_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '使用后余额',
  `store_id` int(11) NOT NULL DEFAULT 0 COMMENT '核销门店ID',
  `technician_id` int(11) NOT NULL DEFAULT 0 COMMENT '核销技师ID',
  `created_at` int(11) NOT NULL DEFAULT 0 COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_card` (`user_card_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员卡核销记录表';

