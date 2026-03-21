SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `booking_review_manager_account_routing`
(
    `id`                    bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `store_id`              bigint      NOT NULL COMMENT '门店ID',
    `manager_admin_user_id` bigint               DEFAULT NULL COMMENT '店长后台账号ID',
    `manager_wecom_user_id` varchar(128)         DEFAULT NULL COMMENT '店长企微账号ID',
    `binding_status`        varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '绑定状态',
    `effective_time`        datetime             DEFAULT NULL COMMENT '生效时间',
    `expire_time`           datetime             DEFAULT NULL COMMENT '失效时间',
    `source`                varchar(64)          DEFAULT '' COMMENT '来源',
    `last_verified_time`    datetime             DEFAULT NULL COMMENT '最近核验时间',
    `creator`               varchar(64)          DEFAULT '' COMMENT '创建者',
    `create_time`           datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`               varchar(64)          DEFAULT '' COMMENT '更新者',
    `update_time`           datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`             bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_booking_review_mgr_route_store_user` (`store_id`, `manager_admin_user_id`, `deleted`),
    KEY `idx_booking_review_mgr_route_store_status` (`store_id`, `binding_status`, `id`)
) COMMENT ='预约评价店长账号路由表';

CREATE TABLE IF NOT EXISTS `booking_review_notify_outbox`
(
    `id`                bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `biz_type`          varchar(64) NOT NULL COMMENT '业务类型',
    `biz_id`            bigint      NOT NULL COMMENT '业务ID',
    `store_id`          bigint      NOT NULL COMMENT '门店ID',
    `receiver_role`     varchar(32) NOT NULL COMMENT '接收角色',
    `receiver_user_id`  bigint               DEFAULT NULL COMMENT '接收账号ID',
    `receiver_account`  varchar(128)         DEFAULT NULL COMMENT '接收账号',
    `notify_type`       varchar(64) NOT NULL COMMENT '通知类型',
    `channel`           varchar(32) NOT NULL COMMENT '通知渠道',
    `status`            varchar(32) NOT NULL COMMENT '状态',
    `retry_count`       int         NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_time`   datetime             DEFAULT NULL COMMENT '下次重试时间',
    `sent_time`         datetime             DEFAULT NULL COMMENT '发送成功时间',
    `last_error_msg`    varchar(512)         DEFAULT '' COMMENT '最后错误信息',
    `idempotency_key`   varchar(128) NOT NULL COMMENT '幂等键',
    `payload_snapshot`  json                 DEFAULT NULL COMMENT '载荷快照',
    `last_action_code`  varchar(64)          DEFAULT NULL COMMENT '最近动作编码',
    `last_action_biz_no` varchar(64)         DEFAULT NULL COMMENT '最近动作业务号',
    `last_action_time`  datetime             DEFAULT NULL COMMENT '最近动作时间',
    `creator`           varchar(64)          DEFAULT '' COMMENT '创建者',
    `create_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`           varchar(64)          DEFAULT '' COMMENT '更新者',
    `update_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`         bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_booking_review_notify_outbox_idempotency` (`idempotency_key`, `deleted`),
    KEY `idx_booking_review_notify_outbox_biz` (`biz_type`, `biz_id`, `id`),
    KEY `idx_booking_review_notify_outbox_status` (`status`, `next_retry_time`, `id`)
) COMMENT ='预约评价通知出站表';

ALTER TABLE `booking_review_manager_account_routing`
    ADD COLUMN IF NOT EXISTS `manager_wecom_user_id` varchar(128) DEFAULT NULL COMMENT '店长企微账号ID' AFTER `manager_admin_user_id`;

ALTER TABLE `booking_review_manager_account_routing`
    MODIFY COLUMN `manager_admin_user_id` bigint DEFAULT NULL COMMENT '店长后台账号ID';

ALTER TABLE `booking_review_notify_outbox`
    ADD COLUMN IF NOT EXISTS `receiver_account` varchar(128) DEFAULT NULL COMMENT '接收账号' AFTER `receiver_user_id`;

SET FOREIGN_KEY_CHECKS = 1;
