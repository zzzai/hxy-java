-- 四账对账：退款佣金审计字段增强（可筛选、可追溯）
ALTER TABLE `hxy_four_account_reconcile`
    ADD COLUMN IF NOT EXISTS `pay_refund_id` BIGINT DEFAULT NULL COMMENT '退款单编号（巡检快照）' AFTER `issue_detail_json`,
    ADD COLUMN IF NOT EXISTS `refund_time` DATETIME DEFAULT NULL COMMENT '退款时间（巡检快照）' AFTER `pay_refund_id`,
    ADD COLUMN IF NOT EXISTS `refund_limit_source` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '退款上限来源（巡检快照）' AFTER `refund_time`,
    ADD COLUMN IF NOT EXISTS `refund_exception_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '退款异常类型（巡检快照）' AFTER `refund_limit_source`,
    ADD COLUMN IF NOT EXISTS `refund_audit_status` VARCHAR(16) NOT NULL DEFAULT 'PASS' COMMENT '退款审计状态（PASS/WARN）' AFTER `refund_exception_type`,
    ADD COLUMN IF NOT EXISTS `refund_audit_remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '退款审计备注' AFTER `refund_audit_status`,
    ADD COLUMN IF NOT EXISTS `refund_evidence_json` TEXT NULL COMMENT '退款审计证据快照 JSON' AFTER `refund_audit_remark`;

CREATE INDEX IF NOT EXISTS `idx_hxy_far_refund_status_date`
    ON `hxy_four_account_reconcile` (`refund_audit_status`, `biz_date`);

CREATE INDEX IF NOT EXISTS `idx_hxy_far_refund_exception_date`
    ON `hxy_four_account_reconcile` (`refund_exception_type`, `biz_date`);

CREATE INDEX IF NOT EXISTS `idx_hxy_far_pay_refund_id`
    ON `hxy_four_account_reconcile` (`pay_refund_id`);

CREATE INDEX IF NOT EXISTS `idx_hxy_far_refund_time`
    ON `hxy_four_account_reconcile` (`refund_time`);
