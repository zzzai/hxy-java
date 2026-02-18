-- 总部退款执行权限策略模板（手工退款入口）
-- 目标：总部可直接执行退款；门店账号仅能进入退款工单池，不可直接打款。
-- 使用方式：
-- 1) 按实际管理员ID/角色ID替换占位值。
-- 2) 执行后重启 admin 服务，确保新配置生效。

-- 是否开启“仅总部可手工退款执行”
UPDATE eb_system_config SET value='1', update_time=NOW() WHERE name='payment_refund_hq_only_enable';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'payment_refund_hq_only_enable', '手工退款仅总部可执行开关(1开0关)', 0, '1', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='payment_refund_hq_only_enable');

-- 总部管理员ID白名单（逗号分隔）
UPDATE eb_system_config SET value='REPLACE_ME_HQ_ADMIN_IDS', update_time=NOW() WHERE name='payment_refund_hq_admin_ids';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'payment_refund_hq_admin_ids', '手工退款总部管理员ID白名单', 0, 'REPLACE_ME_HQ_ADMIN_IDS', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='payment_refund_hq_admin_ids');

-- 总部角色ID白名单（逗号分隔）
UPDATE eb_system_config SET value='REPLACE_ME_HQ_ROLE_IDS', update_time=NOW() WHERE name='payment_refund_hq_role_ids';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'payment_refund_hq_role_ids', '手工退款总部角色白名单', 0, 'REPLACE_ME_HQ_ROLE_IDS', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='payment_refund_hq_role_ids');

-- 自检
SELECT name, value
FROM eb_system_config
WHERE name IN (
  'payment_refund_hq_only_enable',
  'payment_refund_hq_admin_ids',
  'payment_refund_hq_role_ids'
)
ORDER BY name;
