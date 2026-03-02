-- HXY: Enable CRM module schema for MySQL
-- Purpose: remove `[CRM 模块 crm - 已禁用]` by enabling CRM backend + base tables
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `crm_business` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `customer_id` bigint DEFAULT NULL,
  `follow_up_status` bit(1) NOT NULL DEFAULT b'0',
  `contact_last_time` datetime DEFAULT NULL,
  `contact_next_time` datetime DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `status_type_id` bigint DEFAULT NULL,
  `status_id` bigint DEFAULT NULL,
  `end_status` int DEFAULT NULL,
  `end_remark` varchar(1024) DEFAULT '',
  `deal_time` datetime DEFAULT NULL,
  `total_product_price` decimal(24,2) DEFAULT 0.00,
  `discount_percent` decimal(10,2) DEFAULT 0.00,
  `total_price` decimal(24,2) DEFAULT 0.00,
  `remark` varchar(2048) DEFAULT '',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_crm_business_customer_id` (`customer_id`),
  KEY `idx_crm_business_owner_user_id` (`owner_user_id`)
) COMMENT='CRM 商机';

CREATE TABLE IF NOT EXISTS `crm_business_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `product_price` decimal(24,2) DEFAULT 0.00,
  `business_price` decimal(24,2) DEFAULT 0.00,
  `count` decimal(24,2) DEFAULT 0.00,
  `total_price` decimal(24,2) DEFAULT 0.00,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_crm_business_product_business_id` (`business_id`),
  KEY `idx_crm_business_product_product_id` (`product_id`)
) COMMENT='CRM 商机产品关联';

CREATE TABLE IF NOT EXISTS `crm_business_status` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type_id` bigint NOT NULL,
  `name` varchar(255) NOT NULL DEFAULT '',
  `percent` int DEFAULT 0,
  `sort` int DEFAULT 0,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_crm_business_status_type_id` (`type_id`)
) COMMENT='CRM 商机状态';

CREATE TABLE IF NOT EXISTS `crm_business_status_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `dept_ids` varchar(2048) DEFAULT '',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
) COMMENT='CRM 商机状态组';

CREATE TABLE IF NOT EXISTS `crm_clue` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `follow_up_status` bit(1) NOT NULL DEFAULT b'0',
  `contact_last_time` datetime DEFAULT NULL,
  `contact_last_content` varchar(1024) DEFAULT '',
  `contact_next_time` datetime DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `transform_status` bit(1) NOT NULL DEFAULT b'0',
  `customer_id` bigint DEFAULT NULL,
  `mobile` varchar(64) DEFAULT '',
  `telephone` varchar(64) DEFAULT '',
  `qq` varchar(64) DEFAULT '',
  `wechat` varchar(128) DEFAULT '',
  `email` varchar(255) DEFAULT '',
  `area_id` int DEFAULT NULL,
  `detail_address` varchar(1024) DEFAULT '',
  `industry_id` int DEFAULT NULL,
  `level` int DEFAULT NULL,
  `source` int DEFAULT NULL,
  `remark` varchar(2048) DEFAULT '',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_crm_clue_owner_user_id` (`owner_user_id`),
  KEY `idx_crm_clue_customer_id` (`customer_id`)
) COMMENT='CRM 线索';

CREATE TABLE IF NOT EXISTS `crm_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `customer_id` bigint NOT NULL,
  `contact_last_time` datetime DEFAULT NULL,
  `contact_last_content` varchar(1024) DEFAULT '',
  `contact_next_time` datetime DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `mobile` varchar(64) DEFAULT '',
  `telephone` varchar(64) DEFAULT '',
  `email` varchar(255) DEFAULT '',
  `qq` bigint DEFAULT NULL,
  `wechat` varchar(128) DEFAULT '',
  `area_id` int DEFAULT NULL,
  `detail_address` varchar(1024) DEFAULT '',
  `sex` int DEFAULT NULL,
  `master` bit(1) DEFAULT b'0',
  `post` varchar(255) DEFAULT '',
  `parent_id` bigint DEFAULT NULL,
  `remark` varchar(2048) DEFAULT '',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_crm_contact_customer_id` (`customer_id`),
  KEY `idx_crm_contact_owner_user_id` (`owner_user_id`)
) COMMENT='CRM 联系人';

CREATE TABLE IF NOT EXISTS `crm_contact_business` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contact_id` bigint NOT NULL,
  `business_id` bigint NOT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_contact_business` (`contact_id`, `business_id`)
) COMMENT='CRM 联系人与商机关联';

CREATE TABLE IF NOT EXISTS `crm_contract_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `notify_enabled` bit(1) DEFAULT b'0',
  `notify_days` int DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
) COMMENT='CRM 合同配置';

CREATE TABLE IF NOT EXISTS `crm_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `no` varchar(255) NOT NULL DEFAULT '',
  `customer_id` bigint NOT NULL,
  `business_id` bigint DEFAULT NULL,
  `contact_last_time` datetime DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `process_instance_id` varchar(255) DEFAULT '',
  `audit_status` int DEFAULT NULL,
  `order_date` datetime DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `total_product_price` decimal(24,2) DEFAULT 0.00,
  `discount_percent` decimal(10,2) DEFAULT 0.00,
  `total_price` decimal(24,2) DEFAULT 0.00,
  `sign_contact_id` bigint DEFAULT NULL,
  `sign_user_id` bigint DEFAULT NULL,
  `remark` varchar(2048) DEFAULT '',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_crm_contract_customer_id` (`customer_id`),
  KEY `idx_crm_contract_owner_user_id` (`owner_user_id`)
) COMMENT='CRM 合同';

CREATE TABLE IF NOT EXISTS `crm_contract_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contract_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `product_price` decimal(24,2) DEFAULT 0.00,
  `contract_price` decimal(24,2) DEFAULT 0.00,
  `count` decimal(24,2) DEFAULT 0.00,
  `total_price` decimal(24,2) DEFAULT 0.00,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_crm_contract_product_contract_id` (`contract_id`)
) COMMENT='CRM 合同产品关联';

CREATE TABLE IF NOT EXISTS `crm_customer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `follow_up_status` bit(1) NOT NULL DEFAULT b'0',
  `contact_last_time` datetime DEFAULT NULL,
  `contact_last_content` varchar(1024) DEFAULT '',
  `contact_next_time` datetime DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `owner_time` datetime DEFAULT NULL,
  `lock_status` bit(1) NOT NULL DEFAULT b'0',
  `deal_status` bit(1) NOT NULL DEFAULT b'0',
  `mobile` varchar(64) DEFAULT '',
  `telephone` varchar(64) DEFAULT '',
  `qq` varchar(64) DEFAULT '',
  `wechat` varchar(128) DEFAULT '',
  `email` varchar(255) DEFAULT '',
  `area_id` int DEFAULT NULL,
  `detail_address` varchar(1024) DEFAULT '',
  `industry_id` int DEFAULT NULL,
  `level` int DEFAULT NULL,
  `source` int DEFAULT NULL,
  `remark` varchar(2048) DEFAULT '',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_crm_customer_owner_user_id` (`owner_user_id`)
) COMMENT='CRM 客户';

CREATE TABLE IF NOT EXISTS `crm_customer_limit_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` int NOT NULL,
  `user_ids` varchar(2048) DEFAULT '',
  `dept_ids` varchar(2048) DEFAULT '',
  `max_count` int DEFAULT NULL,
  `deal_count_enabled` bit(1) NOT NULL DEFAULT b'0',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
) COMMENT='CRM 客户限制配置';

CREATE TABLE IF NOT EXISTS `crm_customer_pool_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) NOT NULL DEFAULT b'0',
  `contact_expire_days` int DEFAULT NULL,
  `deal_expire_days` int DEFAULT NULL,
  `notify_enabled` bit(1) DEFAULT b'0',
  `notify_days` int DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
) COMMENT='CRM 公海配置';

CREATE TABLE IF NOT EXISTS `crm_follow_up_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` int NOT NULL,
  `biz_id` bigint NOT NULL,
  `type` int DEFAULT NULL,
  `content` text,
  `next_time` datetime DEFAULT NULL,
  `pic_urls` text,
  `file_urls` text,
  `business_ids` text,
  `contact_ids` text,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_crm_follow_up_record_biz` (`biz_type`, `biz_id`)
) COMMENT='CRM 跟进记录';

CREATE TABLE IF NOT EXISTS `crm_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` int NOT NULL,
  `biz_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `level` int NOT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_permission` (`biz_type`, `biz_id`, `user_id`)
) COMMENT='CRM 数据权限';

CREATE TABLE IF NOT EXISTS `crm_product_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `parent_id` bigint DEFAULT 0,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
) COMMENT='CRM 产品分类';

CREATE TABLE IF NOT EXISTS `crm_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `no` varchar(255) NOT NULL DEFAULT '',
  `unit` int DEFAULT NULL,
  `price` decimal(24,2) DEFAULT 0.00,
  `status` int DEFAULT 1,
  `category_id` bigint DEFAULT NULL,
  `description` varchar(2048) DEFAULT '',
  `owner_user_id` bigint DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_crm_product_category_id` (`category_id`)
) COMMENT='CRM 产品';

CREATE TABLE IF NOT EXISTS `crm_receivable` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `no` varchar(255) NOT NULL DEFAULT '',
  `plan_id` bigint DEFAULT NULL,
  `customer_id` bigint NOT NULL,
  `contract_id` bigint DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `return_time` datetime DEFAULT NULL,
  `return_type` int DEFAULT NULL,
  `price` decimal(24,2) DEFAULT 0.00,
  `remark` varchar(2048) DEFAULT '',
  `process_instance_id` varchar(255) DEFAULT '',
  `audit_status` int DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_crm_receivable_customer_id` (`customer_id`),
  KEY `idx_crm_receivable_contract_id` (`contract_id`)
) COMMENT='CRM 回款';

CREATE TABLE IF NOT EXISTS `crm_receivable_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `period` int DEFAULT NULL,
  `customer_id` bigint NOT NULL,
  `contract_id` bigint DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `return_time` datetime DEFAULT NULL,
  `return_type` int DEFAULT NULL,
  `price` decimal(24,2) DEFAULT 0.00,
  `receivable_id` bigint DEFAULT NULL,
  `remind_days` int DEFAULT NULL,
  `remind_time` datetime DEFAULT NULL,
  `remark` varchar(2048) DEFAULT '',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_crm_receivable_plan_customer_id` (`customer_id`),
  KEY `idx_crm_receivable_plan_contract_id` (`contract_id`)
) COMMENT='CRM 回款计划';

SET FOREIGN_KEY_CHECKS = 1;
