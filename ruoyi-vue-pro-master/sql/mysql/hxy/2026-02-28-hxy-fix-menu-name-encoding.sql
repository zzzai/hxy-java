-- HXY: 修复后台商品分路菜单中文乱码
-- 根因：历史执行 SQL 时会话字符集为 latin1，中文字段写入发生乱码

SET NAMES utf8mb4;

UPDATE system_menu
SET name = '服务项目管理',
    updater = '1',
    update_time = NOW()
WHERE type = 2
  AND path = 'service-spu';

UPDATE system_menu
SET name = '实物商品管理',
    updater = '1',
    update_time = NOW()
WHERE type = 2
  AND path = 'physical-spu';

UPDATE system_menu
SET name = '服务项目查询',
    updater = '1',
    update_time = NOW()
WHERE type = 3
  AND permission = 'product:service-spu:query';

UPDATE system_menu
SET name = '服务项目创建',
    updater = '1',
    update_time = NOW()
WHERE type = 3
  AND permission = 'product:service-spu:create';

UPDATE system_menu
SET name = '服务项目更新',
    updater = '1',
    update_time = NOW()
WHERE type = 3
  AND permission = 'product:service-spu:update';

UPDATE system_menu
SET name = '服务项目删除',
    updater = '1',
    update_time = NOW()
WHERE type = 3
  AND permission = 'product:service-spu:delete';

UPDATE system_menu
SET name = '实物商品查询',
    updater = '1',
    update_time = NOW()
WHERE type = 3
  AND permission = 'product:physical-spu:query';

UPDATE system_menu
SET name = '实物商品创建',
    updater = '1',
    update_time = NOW()
WHERE type = 3
  AND permission = 'product:physical-spu:create';

UPDATE system_menu
SET name = '实物商品更新',
    updater = '1',
    update_time = NOW()
WHERE type = 3
  AND permission = 'product:physical-spu:update';

UPDATE system_menu
SET name = '实物商品删除',
    updater = '1',
    update_time = NOW()
WHERE type = 3
  AND permission = 'product:physical-spu:delete';
