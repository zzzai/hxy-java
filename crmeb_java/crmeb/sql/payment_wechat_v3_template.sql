-- 微信支付 v3 配置模板（小程序主链路）
-- 说明：
-- 1) 先替换 REPLACE_ME_* 占位符；
-- 2) 本模板只写配置，不改业务数据；
-- 3) 执行后重启服务或清理配置缓存。

-- =========================
-- 通用开关
-- =========================

UPDATE eb_system_config SET value='v3', update_time=NOW() WHERE name='pay_routine_api_version';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_api_version', '小程序支付API版本(v2/v3)', 0, 'v3', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_api_version');

-- =========================
-- 服务商模式（推荐）
-- =========================
-- 需要和现有服务商参数共同使用：
-- pay_routine_sp_mchid / pay_routine_sub_mchid / pay_routine_sp_appid / pay_routine_sub_appid

UPDATE eb_system_config SET value='REPLACE_ME_SP_APIV3_KEY', update_time=NOW() WHERE name='pay_routine_sp_apiv3_key';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sp_apiv3_key', '小程序服务商APIv3密钥', 0, 'REPLACE_ME_SP_APIV3_KEY', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_sp_apiv3_key');

UPDATE eb_system_config SET value='REPLACE_ME_SP_SERIAL_NO', update_time=NOW() WHERE name='pay_routine_sp_serial_no';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sp_serial_no', '小程序服务商商户证书序列号', 0, 'REPLACE_ME_SP_SERIAL_NO', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_sp_serial_no');

UPDATE eb_system_config SET value='REPLACE_ME_SP_PRIVATE_KEY_PATH', update_time=NOW() WHERE name='pay_routine_sp_private_key_path';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sp_private_key_path', '小程序服务商私钥文件路径(pem)', 0, 'REPLACE_ME_SP_PRIVATE_KEY_PATH', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_sp_private_key_path');

UPDATE eb_system_config SET value='REPLACE_ME_SP_PLATFORM_CERT_PATH', update_time=NOW() WHERE name='pay_routine_sp_platform_cert_path';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sp_platform_cert_path', '小程序服务商微信支付平台证书路径', 0, 'REPLACE_ME_SP_PLATFORM_CERT_PATH', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_sp_platform_cert_path');

-- =========================
-- 直连商户模式（保留）
-- =========================

UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_APIV3_KEY', update_time=NOW() WHERE name='pay_routine_apiv3_key';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_apiv3_key', '小程序直连APIv3密钥', 0, 'REPLACE_ME_ROUTINE_APIV3_KEY', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_apiv3_key');

UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_SERIAL_NO', update_time=NOW() WHERE name='pay_routine_serial_no';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_serial_no', '小程序直连商户证书序列号', 0, 'REPLACE_ME_ROUTINE_SERIAL_NO', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_serial_no');

UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_PRIVATE_KEY_PATH', update_time=NOW() WHERE name='pay_routine_private_key_path';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_private_key_path', '小程序直连私钥文件路径(pem)', 0, 'REPLACE_ME_ROUTINE_PRIVATE_KEY_PATH', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_private_key_path');

UPDATE eb_system_config SET value='REPLACE_ME_ROUTINE_PLATFORM_CERT_PATH', update_time=NOW() WHERE name='pay_routine_platform_cert_path';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_platform_cert_path', '小程序直连微信支付平台证书路径', 0, 'REPLACE_ME_ROUTINE_PLATFORM_CERT_PATH', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_platform_cert_path');

-- =========================
-- 自检
-- =========================
SELECT name, value
FROM eb_system_config
WHERE name IN (
  'pay_routine_api_version',
  'pay_routine_sp_apiv3_key',
  'pay_routine_sp_serial_no',
  'pay_routine_sp_private_key_path',
  'pay_routine_sp_platform_cert_path',
  'pay_routine_apiv3_key',
  'pay_routine_serial_no',
  'pay_routine_private_key_path',
  'pay_routine_platform_cert_path'
)
ORDER BY name;
