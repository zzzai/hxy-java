-- ============================================
-- 荷小悦O2O系统 - 方案B数据库脚本
-- 版本: V2.0 (排班+卡项)
-- 日期: 2026-02-13
-- 说明: 基于V1.0，新增排班和卡项相关表
-- ============================================

-- ============================================
-- 第一部分：卡项体系表（3张）
-- ============================================

-- 1. 卡项模板表（总部定义）
DROP TABLE IF EXISTS `eb_card_template`;
CREATE TABLE `eb_card_template` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '卡项模板ID',
  `name` varchar(128) NOT NULL COMMENT '卡项名称',
  `card_type` tinyint(1) NOT NULL COMMENT '卡型：1=次卡 2=期卡 3=储值卡',
  `total_times` int(11) DEFAULT 0 COMMENT '总次数（次卡专用，0=不限）',
  `total_amount` decimal(10,2) DEFAULT 0.00 COMMENT '总金额（储值卡专用）',
  `bonus_amount` decimal(10,2) DEFAULT 0.00 COMMENT '赠送金额（储值卡专用）',
  `valid_days` int(11) DEFAULT 0 COMMENT '有效期（天，0=永久）',
  `applicable_services` json DEFAULT NULL COMMENT '适用服务JSON',
  `sell_price` decimal(10,2) NOT NULL COMMENT '售卖价格',
  `market_price` decimal(10,2) NOT NULL COMMENT '市场价（原价）',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态：1=在售 0=下架',
  `created_at` int(11) DEFAULT 0,
  `updated_at` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卡项模板表';

-- 2. 用户持卡记录表（核心）
DROP TABLE IF EXISTS `eb_user_card`;
CREATE TABLE `eb_user_card` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '持卡记录ID',
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `card_template_id` int(11) NOT NULL COMMENT '关联卡项模板',
  `card_no` varchar(32) NOT NULL COMMENT '卡号（唯一）',
  `card_type` tinyint(1) NOT NULL COMMENT '卡型（冗余）',
  `purchase_time` int(11) DEFAULT 0 COMMENT '购买时间',
  `expire_time` int(11) DEFAULT 0 COMMENT '过期时间',
  `total_times` int(11) DEFAULT 0 COMMENT '总次数（次卡）',
  `used_times` int(11) DEFAULT 0 COMMENT '已使用次数',
  `remaining_times` int(11) DEFAULT 0 COMMENT '剩余次数',
  `total_amount` decimal(10,2) DEFAULT 0.00 COMMENT '总金额（储值卡）',
  `used_amount` decimal(10,2) DEFAULT 0.00 COMMENT '已使用金额',
  `remaining_amount` decimal(10,2) DEFAULT 0.00 COMMENT '剩余金额',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态：1=正常 2=已用完 3=已过期 4=已冻结',
  `usage_records` json DEFAULT NULL COMMENT '使用记录JSON',
  `created_at` int(11) DEFAULT 0,
  `updated_at` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_card_no` (`card_no`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户持卡记录表';

-- 3. 卡项核销记录表
DROP TABLE IF EXISTS `eb_card_usage`;
CREATE TABLE `eb_card_usage` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_card_id` bigint(20) NOT NULL COMMENT '持卡记录ID',
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `order_id` bigint(20) NOT NULL COMMENT '关联订单ID',
  `usage_type` tinyint(1) NOT NULL COMMENT '核销类型：1=次数 2=金额',
  `usage_times` int(11) DEFAULT 0 COMMENT '核销次数',
  `usage_amount` decimal(10,2) DEFAULT 0.00 COMMENT '核销金额',
  `before_times` int(11) DEFAULT 0 COMMENT '核销前次数',
  `after_times` int(11) DEFAULT 0 COMMENT '核销后次数',
  `before_amount` decimal(10,2) DEFAULT 0.00 COMMENT '核销前金额',
  `after_amount` decimal(10,2) DEFAULT 0.00 COMMENT '核销后金额',
  `technician_id` int(11) DEFAULT 0 COMMENT '服务技师ID',
  `store_id` int(11) DEFAULT 0 COMMENT '门店ID',
  `created_at` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_card` (`user_card_id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卡项核销记录表';

-- ============================================
-- 第二部分：排班相关表扩展
-- ============================================

-- 4. 扩展技师排班表（增加智能排班字段）
ALTER TABLE `eb_technician_schedule`
ADD COLUMN `week_day` tinyint(1) DEFAULT 0 COMMENT '星期几：0=周日 1=周一...6=周六',
ADD COLUMN `is_rest_day` tinyint(1) DEFAULT 0 COMMENT '是否休息日：0=否 1=是',
ADD COLUMN `work_start_time` varchar(10) DEFAULT '' COMMENT '上班时间（如09:00）',
ADD COLUMN `work_end_time` varchar(10) DEFAULT '' COMMENT '下班时间（如22:00）',
ADD COLUMN `predicted_booking_rate` decimal(5,2) DEFAULT 0.00 COMMENT '预测预约率（智能推荐）';

-- 5. 扩展订单表（增加派单和加钟字段）
ALTER TABLE `eb_store_order`
ADD COLUMN `dispatch_mode` tinyint(1) DEFAULT 1 COMMENT '派单模式：1=点钟 2=排钟',
ADD COLUMN `parent_order_id` bigint(20) DEFAULT 0 COMMENT '关联主订单（加钟订单专用）',
ADD COLUMN `is_addon` tinyint(1) DEFAULT 0 COMMENT '是否加钟订单：0=否 1=是',
ADD COLUMN `addon_type` tinyint(1) DEFAULT 0 COMMENT '加钟类型：1=延长时长 2=升级项目 3=增加项目',
ADD COLUMN `original_end_time` int(11) DEFAULT 0 COMMENT '原计划结束时间',
ADD COLUMN `actual_end_time` int(11) DEFAULT 0 COMMENT '实际结束时间（加钟后）',
ADD COLUMN `payment_method` tinyint(1) DEFAULT 1 COMMENT '支付方式：1=微信 2=支付宝 3=会员卡';

-- 6. 技师提成记录表
DROP TABLE IF EXISTS `eb_commission_record`;
CREATE TABLE `eb_commission_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `technician_id` int(11) NOT NULL COMMENT '技师ID',
  `order_id` bigint(20) NOT NULL COMMENT '关联订单ID',
  `commission_type` tinyint(1) NOT NULL COMMENT '提成类型：1=基础服务 2=点钟加成 3=加钟 4=办卡 5=产品 6=好评',
  `base_amount` decimal(10,2) NOT NULL COMMENT '基础金额（订单金额）',
  `commission_rate` decimal(5,4) NOT NULL COMMENT '提成比例（如0.15=15%）',
  `commission_amount` decimal(10,2) NOT NULL COMMENT '提成金额',
  `settlement_status` tinyint(1) DEFAULT 0 COMMENT '结算状态：0=未结算 1=已结算',
  `settlement_time` int(11) DEFAULT 0 COMMENT '结算时间',
  `created_at` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_technician` (`technician_id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_settlement` (`settlement_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师提成记录表';

-- 7. 客情档案表
DROP TABLE IF EXISTS `eb_customer_profile`;
CREATE TABLE `eb_customer_profile` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `preferences` json DEFAULT NULL COMMENT '服务偏好JSON',
  `favorite_technician_id` int(11) DEFAULT 0 COMMENT '最喜欢的技师ID',
  `service_history` json DEFAULT NULL COMMENT '服务历史摘要',
  `tags` varchar(255) DEFAULT '' COMMENT '客户标签',
  `special_notes` text COMMENT '特殊备注',
  `last_visit_time` int(11) DEFAULT 0 COMMENT '最后到店时间',
  `total_visits` int(11) DEFAULT 0 COMMENT '累计到店次数',
  `total_consumption` decimal(10,2) DEFAULT 0.00 COMMENT '累计消费金额',
  `created_at` int(11) DEFAULT 0,
  `updated_at` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客情档案表';

-- ============================================
-- 第三部分：初始化测试数据
-- ============================================

-- 插入卡项模板测试数据
INSERT INTO `eb_card_template` (`name`, `card_type`, `total_times`, `sell_price`, `market_price`, `valid_days`, `status`, `created_at`) VALUES
('10次足疗卡', 1, 10, 980.00, 1280.00, 365, 1, UNIX_TIMESTAMP()),
('月卡无限次', 2, 0, 599.00, 999.00, 30, 1, UNIX_TIMESTAMP()),
('储值卡1000送200', 3, 0, 1000.00, 1000.00, 0, 1, UNIX_TIMESTAMP());

-- ============================================
-- 第四部分：视图和索引优化
-- ============================================

-- 创建卡项使用统计视图
CREATE OR REPLACE VIEW `v_card_usage_stats` AS
SELECT 
  uc.id,
  uc.user_id,
  uc.card_no,
  ct.name AS card_name,
  uc.card_type,
  CASE 
    WHEN uc.card_type = 1 THEN CONCAT(uc.remaining_times, '/', uc.total_times, '次')
    WHEN uc.card_type = 3 THEN CONCAT('¥', uc.remaining_amount, '/¥', uc.total_amount)
    ELSE '无限次'
  END AS usage_info,
  uc.status,
  uc.expire_time
FROM `eb_user_card` uc
LEFT JOIN `eb_card_template` ct ON uc.card_template_id = ct.id;

-- 创建技师提成统计视图
CREATE OR REPLACE VIEW `v_technician_commission_stats` AS
SELECT 
  t.id AS technician_id,
  t.name AS technician_name,
  COUNT(cr.id) AS total_orders,
  SUM(cr.commission_amount) AS total_commission,
  SUM(CASE WHEN cr.settlement_status = 0 THEN cr.commission_amount ELSE 0 END) AS unsettled_amount,
  SUM(CASE WHEN cr.settlement_status = 1 THEN cr.commission_amount ELSE 0 END) AS settled_amount
FROM `eb_technician` t
LEFT JOIN `eb_commission_record` cr ON t.id = cr.technician_id
GROUP BY t.id;

-- ============================================
-- 脚本执行完成
-- ============================================

SELECT '✅ V2.0数据库改造完成！' AS message;
SELECT '新增表: 5个（卡项3个 + 提成1个 + 客情1个）' AS summary;
SELECT '扩展表: 2个（排班表 + 订单表）' AS summary;
SELECT '初始化数据: 3个卡项模板' AS summary;

