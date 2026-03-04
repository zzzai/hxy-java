SET NAMES utf8mb4;

-- HXY: 售后人工复核工单 SLA 预警通知出站（最小闭环）

CREATE TABLE IF NOT EXISTS `trade_after_sale_review_ticket_notify_outbox`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `ticket_id`       bigint       NOT NULL COMMENT '工单 ID',
    `notify_type`     varchar(32)  NOT NULL DEFAULT '' COMMENT '通知类型：SLA_WARN / SLA_ESCALATE',
    `channel`         varchar(32)  NOT NULL DEFAULT 'IN_APP' COMMENT '通知渠道',
    `severity`        varchar(16)  NOT NULL DEFAULT 'P1' COMMENT '严重级别',
    `escalate_to`     varchar(64)  NOT NULL DEFAULT '' COMMENT '升级对象快照',
    `biz_key`         varchar(64)  NOT NULL COMMENT '幂等业务键',
    `status`          tinyint      NOT NULL DEFAULT 0 COMMENT '0 待发送 1 已发送 2 发送失败',
    `retry_count`     int          NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_time` datetime              DEFAULT NULL COMMENT '下次重试时间',
    `sent_time`       datetime              DEFAULT NULL COMMENT '发送成功时间',
    `last_error_msg`  varchar(255) NOT NULL DEFAULT '' COMMENT '最后错误信息',
    `last_action_code` varchar(64) NOT NULL DEFAULT '' COMMENT '最近审计动作编码',
    `last_action_biz_no` varchar(64) NOT NULL DEFAULT '' COMMENT '最近审计业务号',
    `last_action_time` datetime             DEFAULT NULL COMMENT '最近审计动作时间',
    `creator`         varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`         varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`       bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trade_after_sale_review_ticket_notify_outbox_biz_key` (`biz_key`),
    KEY `idx_trade_after_sale_review_ticket_notify_outbox_ticket_status` (`ticket_id`, `status`),
    KEY `idx_trade_after_sale_review_ticket_notify_outbox_dispatch` (`status`, `next_retry_time`, `id`)
) COMMENT ='售后人工复核工单通知出站表';

INSERT INTO infra_config (`category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `updater`, `deleted`)
SELECT 'mall.trade.review-ticket.sla', 2, seed.name, seed.config_key, seed.default_value, b'0',
       '售后人工复核工单 SLA 预警/通知运行参数', '1', '1', b'0'
FROM (
         SELECT '工单SLA预警任务默认批量' AS name,
                'hxy.trade.review-ticket.sla.warn.job.batch-limit.default' AS config_key,
                '200' AS default_value
         UNION ALL
         SELECT '工单SLA预警任务最大批量',
                'hxy.trade.review-ticket.sla.warn.job.batch-limit.max',
                '1000'
         UNION ALL
         SELECT '工单SLA通知分发任务默认批量',
                'hxy.trade.review-ticket.sla.notify.job.batch-limit.default',
                '200'
         UNION ALL
         SELECT '工单SLA通知分发任务最大批量',
                'hxy.trade.review-ticket.sla.notify.job.batch-limit.max',
                '1000'
         UNION ALL
         SELECT '工单SLA预警提前分钟默认值',
                'hxy.trade.review-ticket.sla.warn.lead-minutes.default',
                '30'
         UNION ALL
         SELECT '工单SLA预警提前分钟最大值',
                'hxy.trade.review-ticket.sla.warn.lead-minutes.max',
                '1440'
         UNION ALL
         SELECT '工单SLA通知分发最大重试次数',
                'hxy.trade.review-ticket.sla.notify.max-retry-count',
                '5'
     ) seed
WHERE NOT EXISTS (
    SELECT 1 FROM infra_config c
    WHERE c.config_key = seed.config_key
      AND c.deleted = 0
);
