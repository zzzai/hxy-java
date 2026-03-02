SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `pay_wallet` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `user_type` tinyint NOT NULL,
  `balance` int NOT NULL DEFAULT '0',
  `freeze_price` int NOT NULL DEFAULT '0',
  `total_expense` int NOT NULL DEFAULT '0',
  `total_recharge` int NOT NULL DEFAULT '0',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_wallet_user_type_tenant` (`user_id`, `user_type`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员钱包';

CREATE TABLE IF NOT EXISTS `pay_wallet_recharge_package` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `pay_price` int NOT NULL,
  `bonus_price` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_pay_wallet_recharge_package_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包充值套餐';

CREATE TABLE IF NOT EXISTS `pay_wallet_recharge` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `wallet_id` bigint NOT NULL,
  `total_price` int NOT NULL,
  `pay_price` int NOT NULL,
  `bonus_price` int NOT NULL DEFAULT '0',
  `package_id` bigint DEFAULT NULL,
  `pay_status` bit(1) NOT NULL DEFAULT b'0',
  `pay_order_id` bigint DEFAULT NULL,
  `pay_channel_code` varchar(32) DEFAULT NULL,
  `pay_time` datetime DEFAULT NULL,
  `pay_refund_id` bigint DEFAULT NULL,
  `refund_total_price` int DEFAULT NULL,
  `refund_pay_price` int DEFAULT NULL,
  `refund_bonus_price` int DEFAULT NULL,
  `refund_time` datetime DEFAULT NULL,
  `refund_status` tinyint DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_pay_wallet_recharge_wallet_pay_time` (`wallet_id`, `pay_status`, `pay_time`),
  KEY `idx_pay_wallet_recharge_refund_time` (`refund_status`, `refund_time`),
  KEY `idx_pay_wallet_recharge_pay_order_id` (`pay_order_id`),
  KEY `idx_pay_wallet_recharge_pay_refund_id` (`pay_refund_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包充值记录';

CREATE TABLE IF NOT EXISTS `pay_wallet_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `no` varchar(64) NOT NULL,
  `wallet_id` bigint NOT NULL,
  `biz_type` tinyint NOT NULL,
  `biz_id` varchar(64) NOT NULL,
  `title` varchar(255) NOT NULL DEFAULT '',
  `price` int NOT NULL,
  `balance` int NOT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_wallet_transaction_no` (`no`),
  KEY `idx_pay_wallet_transaction_wallet_create_time` (`wallet_id`, `create_time`),
  KEY `idx_pay_wallet_transaction_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包流水';

SET FOREIGN_KEY_CHECKS = 1;
