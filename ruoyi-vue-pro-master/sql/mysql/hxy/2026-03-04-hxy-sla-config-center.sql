SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY SLA 工单规则配置中心（booking/trade）
-- 日期：2026-03-04
-- 目标：
-- 1) 统一 SLA 规则表驱动（RULE > TYPE_SEVERITY > TYPE_DEFAULT > GLOBAL_DEFAULT）
-- 2) 支持按工单类型 / 严重级别 / 门店范围匹配
-- 3) 规则启停、优先级、审计动作可追踪
-- =============================================

CREATE TABLE IF NOT EXISTS `trade_ticket_sla_rule`
(
    `id`                   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `ticket_type`          INT         NOT NULL DEFAULT 0 COMMENT '工单类型：0全局默认 10售后复核 20服务履约 30提成争议 40预约结算 41预约结算通知',
    `rule_code`            VARCHAR(64) NOT NULL DEFAULT '' COMMENT '规则编码（RULE层级）',
    `severity`             VARCHAR(16) NOT NULL DEFAULT '' COMMENT '严重级别（TYPE_SEVERITY层级）',
    `scope_type`           TINYINT     NOT NULL DEFAULT 1 COMMENT '作用域类型：1全局 2门店',
    `scope_store_id`       BIGINT      NOT NULL DEFAULT 0 COMMENT '作用域门店ID（全局=0）',
    `enabled`              BIT(1)      NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `priority`             INT         NOT NULL DEFAULT 0 COMMENT '优先级（同层级按降序）',
    `escalate_to`          VARCHAR(64) NOT NULL DEFAULT '' COMMENT '升级对象',
    `sla_minutes`          INT                  DEFAULT NULL COMMENT 'SLA分钟',
    `warn_lead_minutes`    INT                  DEFAULT NULL COMMENT '预警提前分钟',
    `escalate_delay_minutes` INT                DEFAULT NULL COMMENT '升级延迟分钟',
    `remark`               VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
    `last_action`          VARCHAR(64) NOT NULL DEFAULT '' COMMENT '最近动作',
    `last_action_at`       DATETIME             DEFAULT NULL COMMENT '最近动作时间',
    `creator`              VARCHAR(64)          DEFAULT '' COMMENT '创建者',
    `create_time`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`              VARCHAR(64)          DEFAULT '' COMMENT '更新者',
    `update_time`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`              BIT(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='SLA工单规则配置表';

-- 兼容历史环境：字段补齐（重复执行安全）
ALTER TABLE `trade_ticket_sla_rule`
    ADD COLUMN IF NOT EXISTS `last_action` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '最近动作' AFTER `remark`,
    ADD COLUMN IF NOT EXISTS `last_action_at` DATETIME DEFAULT NULL COMMENT '最近动作时间' AFTER `last_action`;

SET @idx_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE table_schema = DATABASE()
                            AND table_name = 'trade_ticket_sla_rule'
                            AND index_name = 'uk_ticket_sla_scope'),
                   'SELECT 1',
                   'ALTER TABLE trade_ticket_sla_rule ADD UNIQUE INDEX uk_ticket_sla_scope (ticket_type, rule_code, severity, scope_type, scope_store_id, deleted)'
           )
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE table_schema = DATABASE()
                            AND table_name = 'trade_ticket_sla_rule'
                            AND index_name = 'idx_ticket_scope_priority'),
                   'SELECT 1',
                   'ALTER TABLE trade_ticket_sla_rule ADD INDEX idx_ticket_scope_priority (ticket_type, scope_type, scope_store_id, priority)'
           )
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE table_schema = DATABASE()
                            AND table_name = 'trade_ticket_sla_rule'
                            AND index_name = 'idx_enabled_priority'),
                   'SELECT 1',
                   'ALTER TABLE trade_ticket_sla_rule ADD INDEX idx_enabled_priority (enabled, priority)'
           )
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = (
    SELECT IF(
                   EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE table_schema = DATABASE()
                            AND table_name = 'trade_ticket_sla_rule'
                            AND index_name = 'idx_rule_code'),
                   'SELECT 1',
                   'ALTER TABLE trade_ticket_sla_rule ADD INDEX idx_rule_code (rule_code)'
           )
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 初始化默认规则（幂等）
INSERT INTO `trade_ticket_sla_rule` (`ticket_type`, `rule_code`, `severity`, `scope_type`, `scope_store_id`,
                                     `enabled`, `priority`, `escalate_to`, `sla_minutes`, `warn_lead_minutes`,
                                     `escalate_delay_minutes`, `remark`, `last_action`, `last_action_at`)
SELECT 0,
       'GLOBAL_DEFAULT',
       '',
       1,
       0,
       b'1',
       10,
       'HQ_AFTER_SALE',
       120,
       30,
       30,
       '全局默认SLA规则',
       'INIT',
       NOW()
WHERE NOT EXISTS (SELECT 1
                  FROM `trade_ticket_sla_rule`
                  WHERE `ticket_type` = 0
                    AND `rule_code` = 'GLOBAL_DEFAULT'
                    AND `severity` = ''
                    AND `scope_type` = 1
                    AND `scope_store_id` = 0
                    AND `deleted` = b'0');

INSERT INTO `trade_ticket_sla_rule` (`ticket_type`, `rule_code`, `severity`, `scope_type`, `scope_store_id`,
                                     `enabled`, `priority`, `escalate_to`, `sla_minutes`, `warn_lead_minutes`,
                                     `escalate_delay_minutes`, `remark`, `last_action`, `last_action_at`)
SELECT 10,
       '',
       'P0',
       1,
       0,
       b'1',
       90,
       'HQ_RISK_FINANCE',
       30,
       10,
       10,
       '售后复核P0默认规则',
       'INIT',
       NOW()
WHERE NOT EXISTS (SELECT 1
                  FROM `trade_ticket_sla_rule`
                  WHERE `ticket_type` = 10
                    AND `rule_code` = ''
                    AND `severity` = 'P0'
                    AND `scope_type` = 1
                    AND `scope_store_id` = 0
                    AND `deleted` = b'0');

INSERT INTO `trade_ticket_sla_rule` (`ticket_type`, `rule_code`, `severity`, `scope_type`, `scope_store_id`,
                                     `enabled`, `priority`, `escalate_to`, `sla_minutes`, `warn_lead_minutes`,
                                     `escalate_delay_minutes`, `remark`, `last_action`, `last_action_at`)
SELECT 10,
       '',
       '',
       1,
       0,
       b'1',
       60,
       'HQ_AFTER_SALE',
       120,
       30,
       30,
       '售后复核类型默认规则',
       'INIT',
       NOW()
WHERE NOT EXISTS (SELECT 1
                  FROM `trade_ticket_sla_rule`
                  WHERE `ticket_type` = 10
                    AND `rule_code` = ''
                    AND `severity` = ''
                    AND `scope_type` = 1
                    AND `scope_store_id` = 0
                    AND `deleted` = b'0');

INSERT INTO `trade_ticket_sla_rule` (`ticket_type`, `rule_code`, `severity`, `scope_type`, `scope_store_id`,
                                     `enabled`, `priority`, `escalate_to`, `sla_minutes`, `warn_lead_minutes`,
                                     `escalate_delay_minutes`, `remark`, `last_action`, `last_action_at`)
SELECT 10,
       'AUTO_REFUND_EXECUTE_FAIL',
       'P0',
       1,
       0,
       b'1',
       100,
       'PAY_DEVOPS',
       15,
       5,
       5,
       '自动退款执行失败专项规则',
       'INIT',
       NOW()
WHERE NOT EXISTS (SELECT 1
                  FROM `trade_ticket_sla_rule`
                  WHERE `ticket_type` = 10
                    AND `rule_code` = 'AUTO_REFUND_EXECUTE_FAIL'
                    AND `severity` = 'P0'
                    AND `scope_type` = 1
                    AND `scope_store_id` = 0
                    AND `deleted` = b'0');

INSERT INTO `trade_ticket_sla_rule` (`ticket_type`, `rule_code`, `severity`, `scope_type`, `scope_store_id`,
                                     `enabled`, `priority`, `escalate_to`, `sla_minutes`, `warn_lead_minutes`,
                                     `escalate_delay_minutes`, `remark`, `last_action`, `last_action_at`)
SELECT 40,
       '',
       '',
       1,
       0,
       b'1',
       50,
       'HQ_AFTER_SALE',
       120,
       30,
       30,
       '预约结算提审默认规则',
       'INIT',
       NOW()
WHERE NOT EXISTS (SELECT 1
                  FROM `trade_ticket_sla_rule`
                  WHERE `ticket_type` = 40
                    AND `rule_code` = ''
                    AND `severity` = ''
                    AND `scope_type` = 1
                    AND `scope_store_id` = 0
                    AND `deleted` = b'0');

INSERT INTO `trade_ticket_sla_rule` (`ticket_type`, `rule_code`, `severity`, `scope_type`, `scope_store_id`,
                                     `enabled`, `priority`, `escalate_to`, `sla_minutes`, `warn_lead_minutes`,
                                     `escalate_delay_minutes`, `remark`, `last_action`, `last_action_at`)
SELECT 41,
       '',
       '',
       1,
       0,
       b'1',
       40,
       'HQ_RISK_FINANCE',
       120,
       30,
       30,
       '预约结算预警/升级默认规则',
       'INIT',
       NOW()
WHERE NOT EXISTS (SELECT 1
                  FROM `trade_ticket_sla_rule`
                  WHERE `ticket_type` = 41
                    AND `rule_code` = ''
                    AND `severity` = ''
                    AND `scope_type` = 1
                    AND `scope_store_id` = 0
                    AND `deleted` = b'0');

SET FOREIGN_KEY_CHECKS = 1;
