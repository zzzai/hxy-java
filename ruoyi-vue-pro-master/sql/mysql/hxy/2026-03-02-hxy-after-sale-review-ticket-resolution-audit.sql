SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 售后统一工单收口审计字段
ALTER TABLE `trade_after_sale_review_ticket`
    ADD COLUMN IF NOT EXISTS `resolve_action_code` varchar(64) NOT NULL DEFAULT '' COMMENT '收口动作编码' AFTER `resolver_type`,
    ADD COLUMN IF NOT EXISTS `resolve_biz_no` varchar(64) NOT NULL DEFAULT '' COMMENT '收口来源业务号' AFTER `resolve_action_code`;

SET FOREIGN_KEY_CHECKS = 1;
