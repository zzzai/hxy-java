-- ============================================
-- 荷小悦O2O系统 - 合规上线拦截规则补丁（V1）
-- 版本: v1.0
-- 日期: 2026-02-16
-- 说明: 用户数据授权、访问审计、删除闭环、高争议标签治理
-- ============================================

-- ============================================
-- 1) 字段治理目录（字段最小化与可追溯）
-- ============================================

CREATE TABLE IF NOT EXISTS `eb_field_governance_catalog` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `field_code` varchar(64) NOT NULL COMMENT '字段编码',
  `field_name` varchar(100) NOT NULL COMMENT '字段名称',
  `domain` varchar(50) NOT NULL COMMENT '数据域：user/health/service/behavior/tag',
  `sensitivity_level` tinyint(1) NOT NULL DEFAULT 1 COMMENT '敏感级别：1=L1 2=L2 3=L3 4=L4',
  `necessity_level` tinyint(1) NOT NULL DEFAULT 1 COMMENT '必要性：1=必需 2=增强 3=冻结',
  `purpose_code` varchar(64) NOT NULL COMMENT '用途编码',
  `legal_basis` varchar(50) NOT NULL COMMENT '法律基础：contract/separate_consent/legal_obligation',
  `consent_required` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否需单独同意',
  `retention_days` int(11) NOT NULL DEFAULT 365 COMMENT '默认保留天数',
  `default_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '默认启用：0=否 1=是',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` int(11) NOT NULL,
  `updated_at` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_field_code` (`field_code`),
  KEY `idx_domain` (`domain`),
  KEY `idx_necessity` (`necessity_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段治理目录';

-- ============================================
-- 2) 用户授权记录（敏感信息单独同意）
-- ============================================

CREATE TABLE IF NOT EXISTS `eb_user_consent_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `tenant_id` int(11) NOT NULL DEFAULT 0 COMMENT '租户ID',
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `store_id` int(11) NOT NULL DEFAULT 0 COMMENT '门店ID',
  `scenario_code` varchar(64) NOT NULL COMMENT '场景编码：HEALTH_BASIC/PHOTO_TONGUE/PHOTO_FACE',
  `data_scope_json` json DEFAULT NULL COMMENT '授权数据范围JSON',
  `purpose_codes_json` json DEFAULT NULL COMMENT '用途编码列表JSON',
  `policy_version` varchar(32) NOT NULL COMMENT '政策版本',
  `consent_text_hash` char(64) NOT NULL COMMENT '授权文案哈希',
  `consent_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态：0=PENDING 1=GRANTED 2=DENIED 3=WITHDRAWN 4=EXPIRED',
  `granted_at` int(11) DEFAULT NULL COMMENT '授权时间',
  `withdrawn_at` int(11) DEFAULT NULL COMMENT '撤回时间',
  `expire_at` int(11) DEFAULT NULL COMMENT '过期时间',
  `source_channel` varchar(32) NOT NULL DEFAULT 'miniapp' COMMENT '来源渠道',
  `operator_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '操作人类型：1=用户 2=员工 3=系统',
  `operator_id` int(11) DEFAULT NULL COMMENT '操作人ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `created_at` int(11) NOT NULL,
  `updated_at` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_scenario` (`tenant_id`, `user_id`, `scenario_code`),
  KEY `idx_status_expire` (`consent_status`, `expire_at`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户授权记录';

-- ============================================
-- 3) 数据访问工单（L3/L4数据访问审批）
-- ============================================

CREATE TABLE IF NOT EXISTS `eb_data_access_ticket` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `ticket_no` varchar(50) NOT NULL COMMENT '工单号',
  `tenant_id` int(11) NOT NULL DEFAULT 0 COMMENT '租户ID',
  `user_id` int(11) NOT NULL COMMENT '被访问用户ID',
  `applicant_id` int(11) NOT NULL COMMENT '申请人ID',
  `applicant_role` tinyint(1) NOT NULL COMMENT '申请人角色：1=技师 2=店长 3=客服 4=管理员',
  `data_level` tinyint(1) NOT NULL COMMENT '数据级别：1=L1 2=L2 3=L3 4=L4',
  `data_fields_json` json DEFAULT NULL COMMENT '访问字段列表JSON',
  `purpose_code` varchar(64) NOT NULL COMMENT '使用目的编码',
  `reason` varchar(255) DEFAULT NULL COMMENT '访问理由',
  `approval_required` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否需要审批',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态：0=SUBMITTED 1=AUTO_PASS 2=APPROVED 3=REJECTED 4=EXECUTED 5=CLOSED',
  `approver_id` int(11) DEFAULT NULL COMMENT '审批人ID',
  `approved_at` int(11) DEFAULT NULL COMMENT '审批时间',
  `rejected_at` int(11) DEFAULT NULL COMMENT '驳回时间',
  `reject_reason` varchar(255) DEFAULT NULL COMMENT '驳回原因',
  `expire_at` int(11) DEFAULT NULL COMMENT '工单过期时间',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `created_at` int(11) NOT NULL,
  `updated_at` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_no` (`ticket_no`),
  KEY `idx_user_status` (`tenant_id`, `user_id`, `status`),
  KEY `idx_applicant_status` (`applicant_id`, `status`),
  KEY `idx_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据访问工单';

-- ============================================
-- 4) 数据删除工单（冷静期+执行回执）
-- ============================================

CREATE TABLE IF NOT EXISTS `eb_data_deletion_ticket` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `ticket_no` varchar(50) NOT NULL COMMENT '删除工单号',
  `tenant_id` int(11) NOT NULL DEFAULT 0 COMMENT '租户ID',
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `scope_code` varchar(64) NOT NULL COMMENT '删除范围：PROFILE_BASIC/HEALTH_DATA/TAG_DATA/ACCOUNT_CLOSE',
  `scope_json` json DEFAULT NULL COMMENT '删除数据范围JSON',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态：0=SUBMITTED 1=COOLING 2=EXECUTING 3=COMPLETED 4=REJECTED 5=FAILED 6=CANCELED',
  `cooling_until` int(11) DEFAULT NULL COMMENT '冷静期结束时间',
  `requested_at` int(11) NOT NULL COMMENT '申请时间',
  `executed_at` int(11) DEFAULT NULL COMMENT '执行完成时间',
  `legal_hold` tinyint(1) NOT NULL DEFAULT 0 COMMENT '法定保留：0=否 1=是',
  `hold_reason` varchar(255) DEFAULT NULL COMMENT '法定保留原因',
  `result_summary` varchar(255) DEFAULT NULL COMMENT '执行结果摘要',
  `operator_id` int(11) DEFAULT NULL COMMENT '执行人ID',
  `created_at` int(11) NOT NULL,
  `updated_at` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_no` (`ticket_no`),
  KEY `idx_user_status` (`tenant_id`, `user_id`, `status`),
  KEY `idx_cooling_until` (`cooling_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据删除工单';

-- ============================================
-- 5) 标签策略上线拦截规则（高争议标签保留但受控）
-- ============================================

CREATE TABLE IF NOT EXISTS `eb_label_policy` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `label_key` varchar(64) NOT NULL COMMENT '标签键',
  `label_name` varchar(100) NOT NULL COMMENT '标签名称',
  `risk_level` tinyint(1) NOT NULL DEFAULT 1 COMMENT '风险级别：1=G0 2=G1 3=G2',
  `enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用',
  `require_manual_review` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否需人工复核',
  `allow_auto_reach` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否允许自动触达',
  `forbid_auto_decision` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否禁止自动决策',
  `purpose_whitelist_json` json DEFAULT NULL COMMENT '允许用途白名单JSON',
  `expiry_days` int(11) NOT NULL DEFAULT 90 COMMENT '标签有效天数',
  `owner` varchar(64) DEFAULT NULL COMMENT '标签负责人',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` int(11) NOT NULL,
  `updated_at` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_label_key` (`label_key`),
  KEY `idx_risk_enabled` (`risk_level`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签策略上线拦截规则';

-- ============================================
-- 6) 初始化字段治理目录（首批样例）
-- ============================================

INSERT INTO `eb_field_governance_catalog`
(`field_code`, `field_name`, `domain`, `sensitivity_level`, `necessity_level`, `purpose_code`, `legal_basis`, `consent_required`, `retention_days`, `default_enabled`, `remarks`, `created_at`, `updated_at`)
VALUES
('complaint_type', '主诉类型', 'health', 2, 1, 'SERVICE_PLAN', 'separate_consent', 1, 730, 1, '首期必需字段', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('discomfort_score_before', '服务前不适评分', 'health', 2, 1, 'EFFECT_EVAL', 'separate_consent', 1, 730, 1, '首期必需字段', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('blood_pressure', '血压', 'health', 3, 2, 'HEALTH_MONITOR', 'separate_consent', 1, 180, 0, '灰度启用', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('tongue_photo_url', '舌照原图', 'health', 4, 2, 'TCM_ANALYSIS', 'separate_consent', 1, 30, 0, '高敏感，默认关闭', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('social_profile_external', '外部社交画像', 'behavior', 3, 3, 'GROWTH_EXPERIMENT', 'separate_consent', 1, 0, 0, '冻结字段，不可上线', UNIX_TIMESTAMP(), UNIX_TIMESTAMP())
ON DUPLICATE KEY UPDATE
  `updated_at` = VALUES(`updated_at`),
  `remarks` = VALUES(`remarks`);

-- ============================================
-- 7) 初始化标签策略（首批样例）
-- ============================================

INSERT INTO `eb_label_policy`
(`label_key`, `label_name`, `risk_level`, `enabled`, `require_manual_review`, `allow_auto_reach`, `forbid_auto_decision`, `purpose_whitelist_json`, `expiry_days`, `owner`, `remarks`, `created_at`, `updated_at`)
VALUES
('price_sensitivity', '价格敏感度', 2, 1, 1, 1, 1, JSON_ARRAY('COUPON_STRATEGY', 'RECALL_CARE'), 90, 'ops_growth', '仅用于券策略和关怀，不用于差别定价', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('income_level_inferred', '收入层级推断', 2, 0, 1, 0, 1, JSON_ARRAY('SEGMENT_ANALYSIS'), 30, 'ops_strategy', '保留定义，默认关闭', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('complaint_risk_level', '投诉风险等级', 2, 1, 1, 0, 1, JSON_ARRAY('SERVICE_QA_INTERVENTION'), 60, 'service_qa', '仅用于人工服务介入', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('marital_status_inferred', '婚姻状态推断', 3, 0, 1, 0, 1, JSON_ARRAY(), 0, 'compliance', '禁止启用', UNIX_TIMESTAMP(), UNIX_TIMESTAMP())
ON DUPLICATE KEY UPDATE
  `updated_at` = VALUES(`updated_at`),
  `enabled` = VALUES(`enabled`),
  `remarks` = VALUES(`remarks`);

-- ============================================
-- 8) 复用现有敏感访问日志表的建议
-- ============================================
-- 如已存在 eb_sensitive_access_log / eb_sensitive_access_permission，
-- 本补丁与其互补，不覆盖现有表结构。
