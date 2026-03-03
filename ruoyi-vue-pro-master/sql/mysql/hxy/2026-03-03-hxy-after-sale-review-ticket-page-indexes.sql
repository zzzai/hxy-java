SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 售后统一工单分页索引补强：路由维度与时间维度筛选
ALTER TABLE `trade_after_sale_review_ticket`
    ADD INDEX `idx_route_scope_id` (`route_scope`, `route_id`),
    ADD INDEX `idx_status_create_time` (`status`, `create_time`),
    ADD INDEX `idx_last_action_time` (`last_action_time`);

SET FOREIGN_KEY_CHECKS = 1;
