-- 服务商 + 子商户 支付配置模板（CRMEB Java）
-- 使用方式：
-- 1) 将 REPLACE_ME_* 占位符替换为真实值。
-- 2) 按需保留小程序/公众号/H5/App对应区块。
-- 3) 在目标环境执行。
-- 4) 执行后重启应用，或清理系统配置缓存（如有）。

-- =========================
-- 小程序渠道（channel=1）
-- =========================

-- 服务商参数
UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_SP_APPID', update_time=NOW() WHERE name='pay_routine_sp_appid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sp_appid', '小程序服务商AppID', 0, 'REPLACE_ME_ROUTINE_SP_APPID', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_sp_appid');

UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_SP_MCHID', update_time=NOW() WHERE name='pay_routine_sp_mchid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sp_mchid', '小程序服务商商户号', 0, 'REPLACE_ME_ROUTINE_SP_MCHID', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_sp_mchid');

UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_SP_KEY', update_time=NOW() WHERE name='pay_routine_sp_key';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sp_key', '小程序服务商APIv2 Key', 0, 'REPLACE_ME_ROUTINE_SP_KEY', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_sp_key');

UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_SP_CERT_PATH', update_time=NOW() WHERE name='pay_routine_sp_certificate_path';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sp_certificate_path', '小程序服务商退款证书路径', 0, 'REPLACE_ME_ROUTINE_SP_CERT_PATH', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_sp_certificate_path');

-- 子商户默认参数（无门店单独配置时使用）
UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_SUB_MCHID_DEFAULT', update_time=NOW() WHERE name='pay_routine_sub_mchid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sub_mchid', '小程序默认子商户号', 0, 'REPLACE_ME_ROUTINE_SUB_MCHID_DEFAULT', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_sub_mchid');

UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_SUB_APPID_DEFAULT', update_time=NOW() WHERE name='pay_routine_sub_appid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sub_appid', '小程序默认子商户AppID', 0, 'REPLACE_ME_ROUTINE_SUB_APPID_DEFAULT', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_sub_appid');

-- 兼容回退参数（建议填写，便于老逻辑/排障）
UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_BASE_APPID', update_time=NOW() WHERE name='pay_routine_appid';
UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_BASE_MCHID', update_time=NOW() WHERE name='pay_routine_mchid';
UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_BASE_KEY', update_time=NOW() WHERE name='pay_routine_key';
UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_BASE_CERT_PATH', update_time=NOW() WHERE name='pay_routine_certificate_path';

-- 门店个性化子商户示例（按实际门店ID扩展）
-- UPDATE eb_system_config SET value='1900000109', update_time=NOW() WHERE name='pay_routine_sub_mchid_1';
-- INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
-- SELECT 'pay_routine_sub_mchid_1', '门店1子商户号', 0, '1900000109', 1, NOW(), NOW()
-- WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_sub_mchid_1');
--
-- UPDATE eb_system_config SET value='wx1234567890abcdef', update_time=NOW() WHERE name='pay_routine_sub_appid_1';
-- INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
-- SELECT 'pay_routine_sub_appid_1', '门店1子商户AppID', 0, 'wx1234567890abcdef', 1, NOW(), NOW()
-- WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_sub_appid_1');


-- =========================
-- 公众号/H5渠道（channel=0/2）
-- =========================

UPDATE eb_system_config SET value='REPLACE_ME_WEIXIN_SP_APPID', update_time=NOW() WHERE name='pay_weixin_sp_appid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_weixin_sp_appid', '公众号服务商AppID', 0, 'REPLACE_ME_WEIXIN_SP_APPID', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_weixin_sp_appid');

UPDATE eb_system_config SET value='REPLACE_ME_WEIXIN_SP_MCHID', update_time=NOW() WHERE name='pay_weixin_sp_mchid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_weixin_sp_mchid', '公众号服务商商户号', 0, 'REPLACE_ME_WEIXIN_SP_MCHID', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_weixin_sp_mchid');

UPDATE eb_system_config SET value='REPLACE_ME_WEIXIN_SP_KEY', update_time=NOW() WHERE name='pay_weixin_sp_key';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_weixin_sp_key', '公众号服务商APIv2 Key', 0, 'REPLACE_ME_WEIXIN_SP_KEY', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_weixin_sp_key');

UPDATE eb_system_config SET value='REPLACE_ME_WEIXIN_SUB_MCHID_DEFAULT', update_time=NOW() WHERE name='pay_weixin_sub_mchid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_weixin_sub_mchid', '公众号默认子商户号', 0, 'REPLACE_ME_WEIXIN_SUB_MCHID_DEFAULT', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_weixin_sub_mchid');


-- =========================
-- APP渠道（channel=4/5）
-- =========================

UPDATE eb_system_config SET value='REPLACE_ME_APP_SP_APPID', update_time=NOW() WHERE name='pay_weixin_app_sp_appid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_weixin_app_sp_appid', 'APP服务商AppID', 0, 'REPLACE_ME_APP_SP_APPID', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_weixin_app_sp_appid');

UPDATE eb_system_config SET value='REPLACE_ME_APP_SP_MCHID', update_time=NOW() WHERE name='pay_weixin_app_sp_mchid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_weixin_app_sp_mchid', 'APP服务商商户号', 0, 'REPLACE_ME_APP_SP_MCHID', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_weixin_app_sp_mchid');

UPDATE eb_system_config SET value='REPLACE_ME_APP_SP_KEY', update_time=NOW() WHERE name='pay_weixin_app_sp_key';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_weixin_app_sp_key', 'APP服务商APIv2 Key', 0, 'REPLACE_ME_APP_SP_KEY', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_weixin_app_sp_key');

UPDATE eb_system_config SET value='REPLACE_ME_APP_SUB_MCHID_DEFAULT', update_time=NOW() WHERE name='pay_weixin_app_sub_mchid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_weixin_app_sub_mchid', 'APP默认子商户号', 0, 'REPLACE_ME_APP_SUB_MCHID_DEFAULT', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_weixin_app_sub_mchid');


-- =========================
-- 自检：查看关键配置
-- =========================
SELECT name, value
FROM eb_system_config
WHERE name IN (
  'pay_routine_sp_appid','pay_routine_sp_mchid','pay_routine_sp_key','pay_routine_sp_certificate_path',
  'pay_routine_sub_mchid','pay_routine_sub_appid',
  'pay_weixin_sp_appid','pay_weixin_sp_mchid','pay_weixin_sp_key','pay_weixin_sub_mchid',
  'pay_weixin_app_sp_appid','pay_weixin_app_sp_mchid','pay_weixin_app_sp_key','pay_weixin_app_sub_mchid'
)
ORDER BY name;
