SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- HXY 技师佣金结算审批流（状态机 + SLA）
-- 日期：2026-03-01
-- 目标：
-- 1) 技师佣金由“直接结算”升级为“结算单审批流”
-- 2) 支持待审 SLA、驳回回收、审核通过后打款
-- =============================================

ALTER TABLE `technician_commission`
    ADD COLUMN IF NOT EXISTS `store_id` BIGINT DEFAULT NULL COMMENT '门店ID' AFTER `user_id`,
    ADD COLUMN IF NOT EXISTS `settlement_id` BIGINT DEFAULT NULL COMMENT '结算单ID' AFTER `status`;

ALTER TABLE `technician_commission`
    ADD INDEX `idx_technician_commission_store_id` (`store_id`),
    ADD INDEX `idx_technician_commission_settlement_id` (`settlement_id`);

CREATE TABLE IF NOT EXISTS `technician_commission_settlement`
(
    `id`                   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '结算单ID',
    `settlement_no`        VARCHAR(64) NOT NULL DEFAULT '' COMMENT '结算单号',
    `store_id`             BIGINT               DEFAULT NULL COMMENT '门店ID',
    `technician_id`        BIGINT      NOT NULL COMMENT '技师ID',
    `status`               TINYINT     NOT NULL DEFAULT 0 COMMENT '状态 0草稿 10待审 20已审 30驳回 40作废 50已打款',
    `commission_count`     INT         NOT NULL DEFAULT 0 COMMENT '佣金条目数',
    `total_commission_amount` INT      NOT NULL DEFAULT 0 COMMENT '佣金总额（分）',
    `review_submit_time`   DATETIME             DEFAULT NULL COMMENT '提审时间',
    `review_deadline_time` DATETIME             DEFAULT NULL COMMENT '审核SLA截止',
    `review_warned`        BIT(1)      NOT NULL DEFAULT b'0' COMMENT '是否已预警',
    `review_warn_time`     DATETIME             DEFAULT NULL COMMENT '预警时间',
    `review_escalated`     BIT(1)      NOT NULL DEFAULT b'0' COMMENT '是否已升级到P0',
    `review_escalate_time` DATETIME             DEFAULT NULL COMMENT '升级时间',
    `reviewed_time`        DATETIME             DEFAULT NULL COMMENT '审核时间',
    `reviewer_id`          BIGINT               DEFAULT NULL COMMENT '审核人',
    `review_remark`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '审核备注',
    `reject_reason`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '驳回原因',
    `paid_time`            DATETIME             DEFAULT NULL COMMENT '打款时间',
    `payer_id`             BIGINT               DEFAULT NULL COMMENT '打款人',
    `pay_voucher_no`       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '打款凭证号',
    `pay_remark`           VARCHAR(255) NOT NULL DEFAULT '' COMMENT '打款备注',
    `remark`               VARCHAR(255) NOT NULL DEFAULT '' COMMENT '业务备注',
    `creator`              VARCHAR(64)          DEFAULT '' COMMENT '创建者',
    `create_time`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`              VARCHAR(64)          DEFAULT '' COMMENT '更新者',
    `update_time`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`              BIT(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`            BIGINT      NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_settlement_no` (`settlement_no`),
    KEY `idx_technician_status` (`technician_id`, `status`),
    KEY `idx_review_deadline` (`status`, `review_deadline_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师佣金结算单';

ALTER TABLE `technician_commission_settlement`
    ADD COLUMN IF NOT EXISTS `pay_voucher_no` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '打款凭证号' AFTER `payer_id`,
    ADD COLUMN IF NOT EXISTS `review_warned` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否已预警' AFTER `review_deadline_time`,
    ADD COLUMN IF NOT EXISTS `review_warn_time` DATETIME DEFAULT NULL COMMENT '预警时间' AFTER `review_warned`,
    ADD COLUMN IF NOT EXISTS `review_escalated` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否已升级到P0' AFTER `review_warn_time`,
    ADD COLUMN IF NOT EXISTS `review_escalate_time` DATETIME DEFAULT NULL COMMENT '升级时间' AFTER `review_escalated`;

CREATE TABLE IF NOT EXISTS `technician_commission_settlement_log`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `settlement_id` BIGINT      NOT NULL COMMENT '结算单ID',
    `action`        VARCHAR(32) NOT NULL DEFAULT '' COMMENT '动作',
    `from_status`   TINYINT              DEFAULT NULL COMMENT '源状态',
    `to_status`     TINYINT              DEFAULT NULL COMMENT '目标状态',
    `operator_id`   BIGINT               DEFAULT NULL COMMENT '操作人ID',
    `operator_type` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '操作人类型',
    `operate_remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '操作备注',
    `action_time`   DATETIME             DEFAULT NULL COMMENT '操作时间',
    `creator`       VARCHAR(64)          DEFAULT '' COMMENT '创建者',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`       VARCHAR(64)          DEFAULT '' COMMENT '更新者',
    `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       BIT(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`     BIGINT      NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_settlement_action` (`settlement_id`, `action`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师佣金结算单操作日志';

CREATE TABLE IF NOT EXISTS `technician_commission_settlement_notify_outbox`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '出站ID',
    `settlement_id` BIGINT       NOT NULL COMMENT '结算单ID',
    `notify_type`   VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '通知类型',
    `channel`       VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '通知渠道',
    `severity`      VARCHAR(8)   NOT NULL DEFAULT '' COMMENT '优先级',
    `biz_key`       VARCHAR(128) NOT NULL DEFAULT '' COMMENT '幂等键',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0待发送 1已发送 2失败',
    `retry_count`   INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_time` DATETIME            DEFAULT NULL COMMENT '下次重试时间',
    `sent_time`     DATETIME              DEFAULT NULL COMMENT '发送成功时间',
    `last_error_msg` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '最后错误信息',
    `last_action_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '最近审计动作编码',
    `last_action_biz_no` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '最近审计业务号',
    `last_action_time` DATETIME DEFAULT NULL COMMENT '最近审计动作时间',
    `creator`       VARCHAR(64)           DEFAULT '' COMMENT '创建者',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`       VARCHAR(64)           DEFAULT '' COMMENT '更新者',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notify_biz_key` (`biz_key`),
    KEY `idx_notify_status_next_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师佣金结算通知出站表';

SET FOREIGN_KEY_CHECKS = 1;
