-- 工单路由命中留痕：固化命中路由与决策顺序，便于审计追溯
ALTER TABLE `trade_after_sale_review_ticket`
    ADD COLUMN IF NOT EXISTS `route_id` BIGINT NULL COMMENT '命中路由ID（兜底为空）' AFTER `escalate_to`,
    ADD COLUMN IF NOT EXISTS `route_scope` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '命中路由作用域' AFTER `route_id`,
    ADD COLUMN IF NOT EXISTS `route_decision_order` VARCHAR(128) NOT NULL DEFAULT ''
        COMMENT '路由决策顺序快照' AFTER `route_scope`;

