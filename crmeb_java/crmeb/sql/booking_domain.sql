-- ========================================
-- 荷小悦预约域 - 数据库表设计
-- ========================================
-- 创建时间：2026-02-12
-- 说明：DDD单体架构 - 预约域数据表
-- ========================================

-- 预约表
CREATE TABLE IF NOT EXISTS `eb_booking` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `booking_no` varchar(32) NOT NULL COMMENT '预约编号',
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `store_id` int(11) NOT NULL COMMENT '门店ID',
  `service_id` int(11) NOT NULL COMMENT '服务ID',
  `service_name` varchar(100) DEFAULT NULL COMMENT '服务名称',
  `technician_id` int(11) DEFAULT NULL COMMENT '技师ID',
  `technician_name` varchar(50) DEFAULT NULL COMMENT '技师姓名',
  `technician_skill_level` tinyint(1) DEFAULT NULL COMMENT '技师技能等级 1-初级 2-中级 3-高级 4-专家',
  `booking_time` datetime NOT NULL COMMENT '预约时间',
  `duration_minutes` int(11) NOT NULL COMMENT '服务时长（分钟）',
  `time_slot_id` int(11) DEFAULT NULL COMMENT '时间槽ID',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态 0-待确认 1-已确认 2-服务中 3-已完成 4-已取消 5-超时取消',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `pay_amount` decimal(10,2) DEFAULT NULL COMMENT '实付金额',
  `order_id` int(11) DEFAULT NULL COMMENT '关联订单ID',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `service_start_time` datetime DEFAULT NULL COMMENT '服务开始时间',
  `service_end_time` datetime DEFAULT NULL COMMENT '服务完成时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_no` (`booking_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_technician_time` (`technician_id`, `booking_time`),
  KEY `idx_status` (`status`),
  KEY `idx_booking_time` (`booking_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- 时间槽表
CREATE TABLE IF NOT EXISTS `eb_time_slot` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '时间槽ID',
  `technician_id` int(11) NOT NULL COMMENT '技师ID',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `duration_minutes` int(11) NOT NULL COMMENT '时长（分钟）',
  `is_available` tinyint(1) DEFAULT '1' COMMENT '是否可用 1-可用 0-已占用',
  `booking_id` int(11) DEFAULT NULL COMMENT '占用的预约ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_technician_time` (`technician_id`, `start_time`, `end_time`),
  KEY `idx_available` (`is_available`),
  KEY `idx_booking_id` (`booking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时间槽表';

-- 技师表（如果不存在）
CREATE TABLE IF NOT EXISTS `eb_technician` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '技师ID',
  `store_id` int(11) NOT NULL COMMENT '所属门店ID',
  `name` varchar(50) NOT NULL COMMENT '技师姓名',
  `work_no` varchar(20) DEFAULT NULL COMMENT '工号',
  `avatar` varchar(255) DEFAULT '' COMMENT '头像',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `skill_level` tinyint(1) DEFAULT '1' COMMENT '技能等级 1-初级 2-中级 3-高级 4-专家',
  `specialty` varchar(255) DEFAULT NULL COMMENT '专长标签（JSON）',
  `intro` text COMMENT '个人简介',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态 1-在职 0-离职',
  `is_busy` tinyint(1) DEFAULT '0' COMMENT '是否忙碌 1-是 0-否',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师表';

-- 插入测试数据（可选）
-- INSERT INTO `eb_technician` (`store_id`, `name`, `work_no`, `skill_level`, `status`) VALUES
-- (1, '王师傅', 'T001', 3, 1),
-- (1, '李师傅', 'T002', 2, 1),
-- (1, '张师傅', 'T003', 4, 1);


