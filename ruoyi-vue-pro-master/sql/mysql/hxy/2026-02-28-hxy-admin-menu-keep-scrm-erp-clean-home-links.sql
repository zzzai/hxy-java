-- HXY: 保留 ERP/CRM（CRM 重命名为 SCRM），并移除首页外链菜单

SET NAMES utf8mb4;

-- 1) CRM 系统改名为 SCRM 系统
UPDATE system_menu
SET name = 'SCRM 系统',
    updater = '1',
    update_time = NOW()
WHERE deleted = 0
  AND type = 1
  AND path = '/crm';

-- 2) 恢复 CRM + ERP 整棵菜单树可见
WITH RECURSIVE keep_root AS (
    SELECT id
    FROM system_menu
    WHERE deleted = 0
      AND type = 1
      AND path IN ('/crm', '/erp')
), keep_tree AS (
    SELECT id FROM keep_root
    UNION ALL
    SELECT sm.id
    FROM system_menu sm
    JOIN keep_tree kt ON sm.parent_id = kt.id
    WHERE sm.deleted = 0
)
UPDATE system_menu sm
JOIN keep_tree kt ON sm.id = kt.id
SET sm.visible = b'1',
    sm.status = 0,
    sm.updater = '1',
    sm.update_time = NOW();

-- 3) 删除首页外链菜单（作者动态 / Boot / Cloud 开发文档）
UPDATE system_menu
SET visible = b'0',
    status = 1,
    updater = '1',
    update_time = NOW()
WHERE deleted = 0
  AND id IN (1254, 2159, 2160);
