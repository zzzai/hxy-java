SET NAMES utf8mb4;

-- 开启门店商品映射菜单，并挂载可见页面（Vue3）

UPDATE system_menu
SET status = 0,
    visible = b'1',
    update_time = NOW()
WHERE id = 5069
  AND deleted = b'0';

INSERT INTO system_menu (
  id, name, permission, type, sort, parent_id, path, icon, component, component_name,
  status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
)
SELECT 5080, '门店SPU映射', '', 2, 10, 5069, 'store-spu-mapping', 'ep:link',
       'mall/product/store/spu/index', 'MallProductStoreSpuMapping',
       0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM system_menu WHERE id = 5080
);

INSERT INTO system_menu (
  id, name, permission, type, sort, parent_id, path, icon, component, component_name,
  status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
)
SELECT 5081, '门店SKU映射', '', 2, 20, 5069, 'store-sku-mapping', 'ep:scale-to-original',
       'mall/product/store/sku/index', 'MallProductStoreSkuMapping',
       0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM system_menu WHERE id = 5081
);

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, 5080, '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = 5080 AND tenant_id = 1 AND deleted = b'0'
);

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, 5081, '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = 5081 AND tenant_id = 1 AND deleted = b'0'
);

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 2, 5080, '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM system_role_menu WHERE role_id = 2 AND menu_id = 5080 AND tenant_id = 1 AND deleted = b'0'
);

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 2, 5081, '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM system_role_menu WHERE role_id = 2 AND menu_id = 5081 AND tenant_id = 1 AND deleted = b'0'
);
