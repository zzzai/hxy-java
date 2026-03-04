SET NAMES utf8mb4;

-- HXY: 售后工单 SLA 运行参数配置（job 阈值 + fallback 路由）

INSERT INTO infra_config (category, type, name, config_key, value, visible, remark, creator, updater, deleted)
SELECT 'mall.trade.review-ticket.sla', 2, seed.name, seed.config_key, seed.default_value, b'0',
       '售后工单 SLA 运行参数（可在线调整，未配置时走代码默认值）', '1', '1', b'0'
FROM (
         SELECT '工单SLA升级任务默认批量' AS name,
                'hxy.trade.review-ticket.sla.job.batch-limit.default' AS config_key,
                '200' AS default_value
         UNION ALL
         SELECT '工单SLA升级任务最大批量',
                'hxy.trade.review-ticket.sla.job.batch-limit.max',
                '1000'
         UNION ALL
         SELECT '工单SLA全局兜底-P0升级对象',
                'hxy.trade.review-ticket.sla.fallback.p0.escalate-to',
                'HQ_RISK_FINANCE'
         UNION ALL
         SELECT '工单SLA全局兜底-P0时限(分钟)',
                'hxy.trade.review-ticket.sla.fallback.p0.sla-minutes',
                '30'
         UNION ALL
         SELECT '工单SLA全局兜底-默认升级对象',
                'hxy.trade.review-ticket.sla.fallback.default.escalate-to',
                'HQ_AFTER_SALE'
         UNION ALL
         SELECT '工单SLA全局兜底-默认时限(分钟)',
                'hxy.trade.review-ticket.sla.fallback.default.sla-minutes',
                '120'
     ) seed
WHERE NOT EXISTS (
    SELECT 1 FROM infra_config c
    WHERE c.config_key = seed.config_key
      AND c.deleted = 0
);
