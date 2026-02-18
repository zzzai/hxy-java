-- ============================================
-- 荷小悦O2O系统 - 架构修正SQL（生产就绪版）
-- 版本: v2.0
-- 基线: database_migration_v2.0.sql
-- 说明: 增量补丁，修正并发/幂等/隔离问题
-- ============================================

-- ============================================
-- 补丁1：eb_card_usage 增加幂等和并发控制
-- ============================================

ALTER TABLE `eb_card_usage`
ADD COLUMN `idempotent_key` varchar(64) DEFAULT NULL COMMENT '幂等键：order_id+usage_seq',
ADD COLUMN `usage_seq` int(11) DEFAULT 1 COMMENT '同订单核销序号（支持部分核销）',
ADD COLUMN `lock_version` int(11) DEFAULT 0 COMMENT '乐观锁版本号',
ADD COLUMN `status` tinyint(1) DEFAULT 1 COMMENT '状态：1=正常 2=已撤销',
ADD COLUMN `reversed_by` bigint(20) DEFAULT NULL COMMENT '撤销关联记录ID',
ADD UNIQUE KEY `uk_idempotent` (`idempotent_key`),
ADD KEY `idx_status` (`status`);

-- ============================================
-- 补丁2：eb_user_card 增加乐观锁
-- ============================================

ALTER TABLE `eb_user_card`
ADD COLUMN `lock_version` int(11) DEFAULT 0 COMMENT '乐观锁版本号';

-- ============================================
-- 补丁3：加钟冲突处理表
-- ============================================

CREATE TABLE IF NOT EXISTS `eb_addon_conflict` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `addon_order_id` bigint(20) NOT NULL COMMENT '加钟订单ID',
  `conflict_order_id` bigint(20) NOT NULL COMMENT '冲突订单ID',
  `conflict_type` tinyint(1) NOT NULL COMMENT '冲突类型：1=技师 2=房间 3=时间',
  `conflict_snapshot` text COMMENT '冲突快照JSON',
  `resolution` tinyint(1) DEFAULT 0 COMMENT '处理方式：0=待处理 1=换技师 2=换房间 3=顺延 4=拒绝',
  `resolved_by` int(11) DEFAULT NULL COMMENT '处理人ID',
  `resolved_at` datetime DEFAULT NULL COMMENT '处理时间',
  `lock_version` int(11) DEFAULT 0 COMMENT '乐观锁',
  `created_at` int(11) NOT NULL,
  `updated_at` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_addon_order` (`addon_order_id`),
  KEY `idx_conflict_order` (`conflict_order_id`),
  KEY `idx_resolution` (`resolution`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加钟冲突处理表';

-- ============================================
-- 补丁4：订单表增加资源锁字段
-- ============================================

ALTER TABLE `eb_store_order`
ADD COLUMN `resource_lock_key` varchar(100) DEFAULT NULL COMMENT '资源锁键：technician_id:room_id:time_slot',
ADD COLUMN `lock_version` int(11) DEFAULT 0 COMMENT '乐观锁版本号';

-- ============================================
-- 补丁5：提成规则表
-- ============================================

CREATE TABLE IF NOT EXISTS `eb_commission_rule` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码',
  `rule_type` tinyint(1) NOT NULL COMMENT '规则类型：1=基础提成 2=点钟加成 3=加钟加成 4=办卡提成 5=产品提成 6=特殊活动',
  `condition_expr` text COMMENT '条件表达式JSON',
  `commission_type` tinyint(1) NOT NULL COMMENT '提成类型：1=百分比 2=固定金额 3=阶梯式',
  `commission_value` decimal(10,4) NOT NULL COMMENT '提成值',
  `commission_config` text COMMENT '提成配置JSON（用于阶梯式等复杂规则）',
  `priority` int(11) DEFAULT 0 COMMENT '优先级（数字越大越优先）',
  `merge_mode` tinyint(1) DEFAULT 1 COMMENT '合并模式：1=叠加 2=覆盖 3=取最大',
  `start_time` datetime DEFAULT NULL COMMENT '生效开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '生效结束时间',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态：0=禁用 1=启用',
  `store_id` int(11) DEFAULT 0 COMMENT '门店ID（0=全局）',
  `tenant_id` int(11) DEFAULT 0 COMMENT '租户ID（预留）',
  `created_by` int(11) DEFAULT NULL COMMENT '创建人',
  `created_at` int(11) NOT NULL,
  `updated_at` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_store_status` (`store_id`, `status`),
  KEY `idx_rule_type` (`rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提成规则表';

-- ============================================
-- 补丁6：提成记录增加幂等键
-- ============================================

ALTER TABLE `eb_commission_record`
ADD COLUMN `idempotent_key` varchar(100) DEFAULT NULL COMMENT '幂等键：order_id+technician_id+rule_id',
ADD COLUMN `record_no` varchar(64) DEFAULT NULL COMMENT '记录流水号',
ADD UNIQUE KEY `uk_idempotent` (`idempotent_key`);

-- ============================================
-- 补丁7：客情档案表（加密字段）
-- ============================================

ALTER TABLE `eb_customer_profile`
ADD COLUMN `health_encrypted` text COMMENT '加密后的健康信息',
ADD COLUMN `health_authorized` tinyint(1) DEFAULT 0 COMMENT '是否授权：0=未授权 1=已授权',
ADD COLUMN `authorized_at` datetime DEFAULT NULL COMMENT '授权时间',
ADD COLUMN `authorization_expire` datetime DEFAULT NULL COMMENT '授权过期时间';

-- ============================================
-- 补丁8：敏感数据访问日志
-- ============================================

CREATE TABLE `eb_sensitive_access_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL COMMENT '被访问用户ID',
  `accessor_id` int(11) NOT NULL COMMENT '访问者ID',
  `accessor_type` tinyint(1) NOT NULL COMMENT '访问者类型：1=技师 2=店长 3=客服 4=管理员',
  `data_type` varchar(50) NOT NULL COMMENT '数据类型：health/phone/address/id_card',
  `data_field` varchar(100) DEFAULT NULL COMMENT '具体字段',
  `access_reason` varchar(255) DEFAULT NULL COMMENT '访问原因',
  `access_result` tinyint(1) DEFAULT 1 COMMENT '访问结果：1=成功 2=拒绝（未授权）3=拒绝（无权限）',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(255) DEFAULT NULL COMMENT '用户代理',
  `created_at` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_accessor` (`accessor_id`),
  KEY `idx_data_type` (`data_type`),
  KEY `idx_result` (`access_result`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感数据访问日志';

-- ============================================
-- 补丁9：访问权限配置表
-- ============================================

CREATE TABLE `eb_sensitive_access_permission` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `role_type` tinyint(1) NOT NULL COMMENT '角色类型：1=技师 2=店长 3=客服 4=管理员',
  `data_type` varchar(50) NOT NULL COMMENT '数据类型：health/phone/address/id_card',
  `can_read` tinyint(1) DEFAULT 0 COMMENT '是否可读',
  `can_write` tinyint(1) DEFAULT 0 COMMENT '是否可写',
  `require_reason` tinyint(1) DEFAULT 1 COMMENT '是否需要填写原因',
  `require_approval` tinyint(1) DEFAULT 0 COMMENT '是否需要审批',
  `created_at` int(11) NOT NULL,
  `updated_at` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_data` (`role_type`, `data_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感数据访问权限配置';

-- 初始化权限配置
INSERT INTO `eb_sensitive_access_permission` (`role_type`, `data_type`, `can_read`, `can_write`, `require_reason`, `require_approval`, `created_at`, `updated_at`) VALUES
(1, 'health', 1, 0, 1, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()), -- 技师可读健康信息（需填原因）
(2, 'health', 1, 1, 0, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()), -- 店长可读写
(3, 'health', 1, 0, 1, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()), -- 客服可读（需填原因）
(4, 'health', 1, 1, 0, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()); -- 管理员可读写

-- ============================================
-- 补丁10：多租户隔离（核心表）
-- ============================================

ALTER TABLE `eb_user_card`
ADD COLUMN `tenant_id` int(11) DEFAULT 0 COMMENT '租户ID',
ADD KEY `idx_tenant_user` (`tenant_id`, `user_id`);

ALTER TABLE `eb_card_usage`
ADD COLUMN `tenant_id` int(11) DEFAULT 0 COMMENT '租户ID',
ADD KEY `idx_tenant_order` (`tenant_id`, `order_id`);

ALTER TABLE `eb_store_order`
ADD KEY `idx_tenant_store` (`tenant_id`, `store_id`);

ALTER TABLE `eb_technician`
ADD COLUMN `tenant_id` int(11) DEFAULT 0 COMMENT '租户ID',
ADD KEY `idx_tenant_store` (`tenant_id`, `store_id`);

ALTER TABLE `eb_member_card`
ADD COLUMN `tenant_id` int(11) DEFAULT 0 COMMENT '租户ID',
ADD KEY `idx_tenant` (`tenant_id`);

ALTER TABLE `eb_technician_schedule`
ADD COLUMN `tenant_id` int(11) DEFAULT 0 COMMENT '租户ID',
ADD KEY `idx_tenant` (`tenant_id`);

-- ============================================
-- 补丁11：插入默认提成规则
-- ============================================

INSERT INTO `eb_commission_rule` (`rule_name`, `rule_code`, `rule_type`, `condition_expr`, `commission_type`, `commission_value`, `priority`, `merge_mode`, `status`, `store_id`, `tenant_id`, `created_at`, `updated_at`) VALUES
('基础服务提成', 'BASE_SERVICE', 1, '{"conditions":[]}', 1, 0.1500, 1, 1, 1, 0, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('点钟加成', 'DISPATCH_BONUS', 2, '{"conditions":[{"field":"dispatch_mode","operator":"=","value":1}]}', 1, 0.0500, 2, 1, 1, 0, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('加钟提成', 'ADDON_COMMISSION', 3, '{"conditions":[{"field":"is_addon","operator":"=","value":1}]}', 1, 0.2000, 3, 1, 1, 0, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('办卡提成', 'CARD_SALE', 4, '{"conditions":[]}', 1, 0.0500, 1, 1, 1, 0, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('产品销售提成', 'PRODUCT_SALE', 5, '{"conditions":[]}', 1, 0.1000, 1, 1, 1, 0, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP());

-- ============================================
-- 数据迁移（如果有历史数据）
-- ============================================

-- 迁移1：初始化乐观锁版本号
UPDATE `eb_user_card` SET `lock_version` = 0 WHERE `lock_version` IS NULL;
UPDATE `eb_store_order` SET `lock_version` = 0 WHERE `lock_version` IS NULL;

-- 迁移2：补全历史核销记录的幂等键
UPDATE `eb_card_usage`
SET `idempotent_key` = CONCAT(`order_id`, '_', 1),
    `usage_seq` = 1,
    `status` = 1
WHERE `idempotent_key` IS NULL;

-- 迁移3：初始化租户ID（单租户场景默认为0）
UPDATE `eb_user_card` SET `tenant_id` = 0 WHERE `tenant_id` IS NULL;
UPDATE `eb_card_usage` SET `tenant_id` = 0 WHERE `tenant_id` IS NULL;
UPDATE `eb_technician` SET `tenant_id` = 0 WHERE `tenant_id` IS NULL;
UPDATE `eb_member_card` SET `tenant_id` = 0 WHERE `tenant_id` IS NULL;
UPDATE `eb_technician_schedule` SET `tenant_id` = 0 WHERE `tenant_id` IS NULL;

-- ============================================
-- 索引优化
-- ============================================

-- 订单表索引优化
ALTER TABLE `eb_store_order` 
ADD INDEX `idx_technician_status` (`technician_id`, `status`),
ADD INDEX `idx_reserve_date` (`reserve_date`);

-- 技师表索引优化
ALTER TABLE `eb_technician`
ADD INDEX `idx_status` (`status`);

-- ============================================
-- 完成
-- ============================================

-- 验证补丁执行结果
SELECT 
    'eb_card_usage' AS table_name,
    COUNT(*) AS total_records,
    COUNT(DISTINCT idempotent_key) AS unique_idempotent_keys
FROM eb_card_usage
UNION ALL
SELECT 
    'eb_commission_rule' AS table_name,
    COUNT(*) AS total_records,
    NULL
FROM eb_commission_rule;
