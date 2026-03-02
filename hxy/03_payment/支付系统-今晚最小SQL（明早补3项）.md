# 支付系统-今晚最小SQL（明早补3项）

目标：今晚先把配置骨架放好，避免阻塞开发；明早拿到管理员信息后，只改 3 个值即可联调。

## 今晚先执行（安全模式）

```sql
-- 0) 建议先关闭微信支付开关，防止夜间误触真实下单
UPDATE eb_system_config
SET value = CONCAT(CHAR(39),'0',CHAR(39)), update_time = NOW()
WHERE name = 'pay_weixin_open';

INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_weixin_open', '微信支付开关', 67, CONCAT(CHAR(39),'0',CHAR(39)), 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'pay_weixin_open');

-- 1) 你现在已有的 appid 先填好（已填：wx97fb30aed3983c2c）
UPDATE eb_system_config SET value = 'wx97fb30aed3983c2c', update_time = NOW() WHERE name = 'pay_routine_sp_appid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sp_appid', '小程序服务商AppID', 0, 'wx97fb30aed3983c2c', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'pay_routine_sp_appid');

UPDATE eb_system_config SET value = 'wx97fb30aed3983c2c', update_time = NOW() WHERE name = 'pay_routine_sub_appid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sub_appid', '小程序默认子商户AppID', 0, 'wx97fb30aed3983c2c', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'pay_routine_sub_appid');

-- 2) 兼容回退项也先放好（建议与上面保持一致）
UPDATE eb_system_config SET value = 'wx97fb30aed3983c2c', update_time = NOW() WHERE name = 'pay_routine_appid';

-- 3) 先创建“待补”的3个关键项（先置空，明早补）
UPDATE eb_system_config SET value = '', update_time = NOW() WHERE name = 'pay_routine_sp_mchid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sp_mchid', '小程序服务商商户号', 0, '', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'pay_routine_sp_mchid');

UPDATE eb_system_config SET value = '', update_time = NOW() WHERE name = 'pay_routine_sp_key';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sp_key', '小程序服务商APIv2 Key', 0, '', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'pay_routine_sp_key');

UPDATE eb_system_config SET value = '', update_time = NOW() WHERE name = 'pay_routine_sub_mchid';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sub_mchid', '小程序默认子商户号', 0, '', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'pay_routine_sub_mchid');
```

## 明早只补这3个值

```sql
UPDATE eb_system_config SET value = 'REPLACE_ME_SP_MCHID', update_time = NOW() WHERE name = 'pay_routine_sp_mchid';
UPDATE eb_system_config SET value = 'REPLACE_ME_SP_KEY', update_time = NOW() WHERE name = 'pay_routine_sp_key';
UPDATE eb_system_config SET value = 'REPLACE_ME_SUB_MCHID', update_time = NOW() WHERE name = 'pay_routine_sub_mchid';
```

## 门店级映射（后续再补，不影响今晚）

```sql
-- 示例：storeId=12
UPDATE eb_system_config SET value='REPLACE_ME_SUB_MCHID_STORE_12', update_time=NOW() WHERE name='pay_routine_sub_mchid_12';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT 'pay_routine_sub_mchid_12', '门店12子商户号', 0, 'REPLACE_ME_SUB_MCHID_STORE_12', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='pay_routine_sub_mchid_12');
```

## 执行后检查

```sql
SELECT name, value
FROM eb_system_config
WHERE name IN (
  'pay_weixin_open',
  'pay_routine_sp_appid',
  'pay_routine_sp_mchid',
  'pay_routine_sp_key',
  'pay_routine_sub_appid',
  'pay_routine_sub_mchid',
  'pay_routine_appid'
)
ORDER BY name;
```

## 说明
- 今晚这套配置用于“先占位、先开发”。
- 在 `pay_weixin_open` 关闭期间，不会触发真实微信支付。
- 明早补完 3 项后，重启服务，再开启 `pay_weixin_open` 即可进入支付联调。
