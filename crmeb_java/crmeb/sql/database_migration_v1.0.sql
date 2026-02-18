-- ============================================
-- 荷小悦O2O系统 - 融合方案数据库脚本
-- 版本: V1.0 (修复版)
-- 日期: 2026-02-13
-- 说明: 融合CRMEB + O2O核心技术
-- 修复: 移除重复字段 store_id
-- ============================================

-- ============================================
-- 第一部分：新增核心表
-- ============================================

-- 1. 技师基础信息表（O2O设计）
DROP TABLE IF EXISTS `eb_technician`;
CREATE TABLE `eb_technician` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '技师ID',
  `store_id` int(11) NOT NULL COMMENT '所属门店ID',
  `name` varchar(50) NOT NULL COMMENT '技师姓名',
  `avatar` varchar(255) DEFAULT '' COMMENT '技师头像URL',
  `level` tinyint(1) DEFAULT 1 COMMENT '技师等级：1=初级 2=中级 3=高级 4=首席',
  `service_years` decimal(4,1) DEFAULT 0.0 COMMENT '从业年限',
  `skill_tags` varchar(255) DEFAULT '' COMMENT '技能标签（逗号分隔）',
  `intro` text COMMENT '技师介绍',
  `rating` decimal(3,2) DEFAULT 5.00 COMMENT '评分（5.00满分）',
  `order_count` int(11) DEFAULT 0 COMMENT '累计服务单数',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态：1=在职 2=离职',
  `created_at` int(11) DEFAULT 0 COMMENT '创建时间',
  `updated_at` int(11) DEFAULT 0 COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师基础信息表';

-- 2. 技师排班表（O2O时间槽JSON设计）
DROP TABLE IF EXISTS `eb_technician_schedule`;
CREATE TABLE `eb_technician_schedule` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '排班ID',
  `store_id` int(11) NOT NULL COMMENT '门店ID',
  `technician_id` int(11) NOT NULL COMMENT '技师ID',
  `service_sku_id` int(11) NOT NULL COMMENT '服务SKU ID',
  `work_date` date NOT NULL COMMENT '排班日期',
  `time_slots` json NOT NULL COMMENT '时间槽JSON（核心字段）',
  `total_slots` int(11) DEFAULT 0 COMMENT '总时间槽数',
  `available_slots` int(11) DEFAULT 0 COMMENT '可预约时间槽数',
  `status` tinyint(1) DEFAULT 1 COMMENT '排班状态：1=正常 2=请假 3=已完成',
  `is_offpeak_enabled` tinyint(1) DEFAULT 0 COMMENT '是否启用闲时优惠',
  `created_at` int(11) DEFAULT 0,
  `updated_at` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_store_date` (`store_id`,`work_date`),
  KEY `idx_technician_date` (`technician_id`,`work_date`),
  KEY `idx_sku` (`service_sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师排班表';

-- 3. 库存变动流水表（O2O设计）
DROP TABLE IF EXISTS `eb_stock_flow`;
CREATE TABLE `eb_stock_flow` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `store_id` int(11) NOT NULL COMMENT '门店ID',
  `sku_id` int(11) NOT NULL COMMENT 'SKU ID',
  `order_id` bigint(20) DEFAULT 0 COMMENT '关联订单ID',
  `change_type` tinyint(1) NOT NULL COMMENT '变动类型：1=入库 2=锁定 3=释放 4=扣减 5=退款',
  `change_quantity` int(11) NOT NULL COMMENT '变动数量',
  `before_available` int(11) DEFAULT 0 COMMENT '变动前可售库存',
  `after_available` int(11) DEFAULT 0 COMMENT '变动后可售库存',
  `before_locked` int(11) DEFAULT 0 COMMENT '变动前锁定库存',
  `after_locked` int(11) DEFAULT 0 COMMENT '变动后锁定库存',
  `operator_id` int(11) DEFAULT 0 COMMENT '操作人ID',
  `operator_type` tinyint(1) DEFAULT 1 COMMENT '操作人类型：1=系统 2=店长 3=用户',
  `remark` varchar(255) DEFAULT '' COMMENT '备注',
  `created_at` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_store_sku` (`store_id`,`sku_id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动流水表';

-- 4. 会员卡表（新增）
DROP TABLE IF EXISTS `eb_member_card`;
CREATE TABLE `eb_member_card` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uid` int(11) NOT NULL COMMENT '用户ID',
  `card_type` tinyint(1) NOT NULL COMMENT '卡类型：1=次卡 2=储值卡 3=时长卡',
  `card_name` varchar(100) NOT NULL COMMENT '卡名称',
  `total_value` decimal(10,2) DEFAULT 0.00 COMMENT '总价值（金额/次数/时长）',
  `remaining_value` decimal(10,2) DEFAULT 0.00 COMMENT '剩余价值',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态：1=正常 2=冻结 3=已用完 4=已过期',
  `expire_time` int(11) DEFAULT 0 COMMENT '过期时间',
  `created_at` int(11) DEFAULT 0,
  `updated_at` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_uid` (`uid`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员卡表';

-- 5. 闲时优惠规则表（新增）
DROP TABLE IF EXISTS `eb_offpeak_rule`;
CREATE TABLE `eb_offpeak_rule` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `store_id` int(11) NOT NULL COMMENT '门店ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `week_days` varchar(50) DEFAULT '' COMMENT '生效星期（1,2,3）',
  `time_ranges` json NOT NULL COMMENT '时间段JSON',
  `discount_type` tinyint(1) DEFAULT 1 COMMENT '优惠类型：1=折扣 2=减免',
  `discount_value` decimal(10,2) DEFAULT 0.00 COMMENT '优惠值',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态：1=启用 2=禁用',
  `created_at` int(11) DEFAULT 0,
  `updated_at` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闲时优惠规则表';

-- 6. 预约订单表（新增，独立于商品订单）
DROP TABLE IF EXISTS `eb_booking_order`;
CREATE TABLE `eb_booking_order` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `uid` int(11) NOT NULL COMMENT '用户ID',
  `store_id` int(11) NOT NULL COMMENT '门店ID',
  `technician_id` int(11) NOT NULL COMMENT '技师ID',
  `schedule_id` int(11) NOT NULL COMMENT '排班ID',
  `slot_id` varchar(50) NOT NULL COMMENT '时间槽ID',
  `service_sku_id` int(11) NOT NULL COMMENT '服务SKU ID',
  `service_name` varchar(100) NOT NULL COMMENT '服务名称',
  `reserve_date` date NOT NULL COMMENT '预约日期',
  `reserve_time` varchar(20) NOT NULL COMMENT '预约时间',
  `service_duration` int(11) DEFAULT 60 COMMENT '服务时长（分钟）',
  `original_price` decimal(10,2) DEFAULT 0.00 COMMENT '原价',
  `actual_price` decimal(10,2) DEFAULT 0.00 COMMENT '实付价',
  `offpeak_discount` decimal(10,2) DEFAULT 0.00 COMMENT '闲时优惠',
  `payment_type` tinyint(1) DEFAULT 1 COMMENT '支付方式：1=微信 2=支付宝 3=会员卡',
  `member_card_id` bigint(20) DEFAULT 0 COMMENT '会员卡ID',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态：1=待支付 2=已支付 3=已核销 4=已取消 5=已退款',
  `check_in_code` varchar(32) DEFAULT '' COMMENT '核销码',
  `check_in_time` int(11) DEFAULT 0 COMMENT '核销时间',
  `locked_expire` int(11) DEFAULT 0 COMMENT '锁定过期时间',
  `created_at` int(11) DEFAULT 0,
  `updated_at` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_uid` (`uid`),
  KEY `idx_store_date` (`store_id`,`reserve_date`),
  KEY `idx_schedule` (`schedule_id`),
  KEY `idx_check_in` (`check_in_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约订单表';

-- ============================================
-- 第二部分：扩展CRMEB现有表
-- ============================================

-- 7. 扩展订单表（融合CRMEB + O2O）
-- 注意：store_id 已存在于CRMEB原表，无需重复添加
ALTER TABLE `eb_store_order`
ADD COLUMN `order_type` tinyint(1) DEFAULT '1' COMMENT '订单类型:1=商品 2=服务预约',
ADD COLUMN `technician_id` int(11) DEFAULT '0' COMMENT '技师ID',
ADD COLUMN `technician_name` varchar(50) DEFAULT '' COMMENT '技师姓名',
ADD COLUMN `reserve_date` date DEFAULT NULL COMMENT '预约日期',
ADD COLUMN `reserve_time_slot` varchar(20) DEFAULT '' COMMENT '预约时段(14:00)',
ADD COLUMN `service_duration` int(11) DEFAULT '60' COMMENT '服务时长(分钟)',
ADD COLUMN `check_in_code` varchar(32) DEFAULT '' COMMENT '核销码',
ADD COLUMN `check_in_time` int(11) DEFAULT '0' COMMENT '核销时间',
ADD COLUMN `schedule_id` int(11) DEFAULT '0' COMMENT '关联排班ID',
ADD COLUMN `slot_id` varchar(50) DEFAULT '' COMMENT '时间槽ID',
ADD COLUMN `locked_expire` int(11) DEFAULT '0' COMMENT '锁定过期时间',
ADD COLUMN `service_start_time` int(11) DEFAULT '0' COMMENT '服务开始时间',
ADD COLUMN `service_end_time` int(11) DEFAULT '0' COMMENT '服务结束时间';

-- 添加订单表索引
ALTER TABLE `eb_store_order`
ADD INDEX `idx_order_type` (`order_type`),
ADD INDEX `idx_store_date` (`store_id`, `reserve_date`),
ADD INDEX `idx_schedule` (`schedule_id`),
ADD INDEX `idx_check_in` (`check_in_code`);

-- 8. 扩展商品SKU表（库存管理）
ALTER TABLE `eb_store_product_attr_value`
ADD COLUMN `available_stock` int(11) DEFAULT 0 COMMENT '可售库存',
ADD COLUMN `locked_stock` int(11) DEFAULT 0 COMMENT '锁定库存',
ADD COLUMN `sold_stock` int(11) DEFAULT 0 COMMENT '已售库存',
ADD COLUMN `total_stock` int(11) DEFAULT 0 COMMENT '总库存',
ADD COLUMN `low_stock_threshold` int(11) DEFAULT 10 COMMENT '低库存阈值';

-- 9. 扩展订单详情表（部分核销）
ALTER TABLE `eb_store_order_info`
ADD COLUMN `total_quantity` int(11) NOT NULL DEFAULT 1 COMMENT '购买总数量',
ADD COLUMN `used_quantity` int(11) DEFAULT 0 COMMENT '已核销数量',
ADD COLUMN `remaining_quantity` int(11) NOT NULL DEFAULT 1 COMMENT '剩余数量',
ADD COLUMN `verification_status` tinyint(1) DEFAULT 0 COMMENT '核销状态：0=未核销 1=部分核销 2=已完成',
ADD COLUMN `verification_records` json DEFAULT NULL COMMENT '核销记录JSON',
ADD COLUMN `expire_time` int(11) DEFAULT 0 COMMENT '过期时间';

-- ============================================
-- 第三部分：初始化测试数据
-- ============================================

-- 插入测试技师数据
INSERT INTO `eb_technician` (`store_id`, `name`, `avatar`, `level`, `service_years`, `skill_tags`, `intro`, `rating`, `order_count`, `status`, `created_at`, `updated_at`) VALUES
(1, '王师傅', '/static/tech/tech1.jpg', 3, 5.0, '推拿,刮痧,艾灸', '从业5年，擅长肩颈调理和祛湿艾灸', 4.90, 1200, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
(1, '李师傅', '/static/tech/tech2.jpg', 2, 3.0, '足疗,推拿', '从业3年，手法专业，服务细致', 4.80, 800, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
(1, '张师傅', '/static/tech/tech3.jpg', 4, 8.0, '推拿,刮痧,艾灸,拔罐', '首席技师，从业8年，技术全面', 4.95, 2000, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP());

-- ============================================
-- 第四部分：数据迁移脚本
-- ============================================

-- 迁移现有库存数据到新字段
UPDATE `eb_store_product_attr_value`
SET 
  `available_stock` = `stock`,
  `total_stock` = `stock`,
  `locked_stock` = 0,
  `sold_stock` = 0
WHERE `stock` > 0;

-- ============================================
-- 第五部分：视图和存储过程（可选）
-- ============================================

-- 创建库存汇总视图
CREATE OR REPLACE VIEW `v_stock_summary` AS
SELECT 
  `id` as `sku_id`,
  `available_stock`,
  `locked_stock`,
  `sold_stock`,
  `total_stock`,
  CASE 
    WHEN `available_stock` <= `low_stock_threshold` THEN 1
    ELSE 0
  END AS `is_low_stock`
FROM `eb_store_product_attr_value`;

-- ============================================
-- 脚本执行完成
-- ============================================

-- 验证表是否创建成功
SELECT 
  TABLE_NAME,
  TABLE_COMMENT,
  CREATE_TIME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
    'eb_technician',
    'eb_technician_schedule',
    'eb_stock_flow',
    'eb_member_card',
    'eb_offpeak_rule',
    'eb_booking_order'
  );

-- 验证字段是否添加成功
SELECT 
  TABLE_NAME,
  COLUMN_NAME,
  COLUMN_TYPE,
  COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'eb_store_order'
  AND COLUMN_NAME IN (
    'order_type',
    'schedule_id',
    'slot_id',
    'check_in_code'
  );

-- 显示执行结果
SELECT '✅ 数据库改造完成！' AS message;
SELECT '新增表: 6个' AS summary;
SELECT '扩展表: 3个' AS summary;
SELECT '初始化数据: 3条技师记录' AS summary;
