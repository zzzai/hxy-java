SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `booking_review`
    ADD COLUMN `negative_trigger_type` varchar(32) DEFAULT NULL COMMENT '差评触发类型' AFTER `submit_time`,
    ADD COLUMN `manager_contact_name` varchar(64) DEFAULT NULL COMMENT '店长联系人姓名快照' AFTER `negative_trigger_type`,
    ADD COLUMN `manager_contact_mobile` varchar(32) DEFAULT NULL COMMENT '店长联系人手机号快照' AFTER `manager_contact_name`,
    ADD COLUMN `manager_todo_status` tinyint DEFAULT NULL COMMENT '店长待办状态：1待认领 2已认领 3处理中 4已闭环' AFTER `manager_contact_mobile`,
    ADD COLUMN `manager_claim_deadline_at` datetime DEFAULT NULL COMMENT '店长待办认领截止时间' AFTER `manager_todo_status`,
    ADD COLUMN `manager_first_action_deadline_at` datetime DEFAULT NULL COMMENT '店长待办首次处理截止时间' AFTER `manager_claim_deadline_at`,
    ADD COLUMN `manager_close_deadline_at` datetime DEFAULT NULL COMMENT '店长待办闭环截止时间' AFTER `manager_first_action_deadline_at`,
    ADD COLUMN `manager_claimed_by_user_id` bigint DEFAULT NULL COMMENT '店长待办认领操作人' AFTER `manager_close_deadline_at`,
    ADD COLUMN `manager_claimed_at` datetime DEFAULT NULL COMMENT '店长待办认领时间' AFTER `manager_claimed_by_user_id`,
    ADD COLUMN `manager_first_action_at` datetime DEFAULT NULL COMMENT '店长待办首次处理时间' AFTER `manager_claimed_at`,
    ADD COLUMN `manager_closed_at` datetime DEFAULT NULL COMMENT '店长待办闭环时间' AFTER `manager_first_action_at`,
    ADD COLUMN `manager_latest_action_remark` varchar(500) DEFAULT NULL COMMENT '店长待办最近处理备注' AFTER `manager_closed_at`,
    ADD COLUMN `manager_latest_action_by_user_id` bigint DEFAULT NULL COMMENT '店长待办最近处理人' AFTER `manager_latest_action_remark`;

ALTER TABLE `booking_review`
    ADD KEY `idx_booking_review_manager_todo_status` (`manager_todo_status`, `id`),
    ADD KEY `idx_booking_review_manager_close_deadline` (`manager_close_deadline_at`, `id`);

SET FOREIGN_KEY_CHECKS = 1;
