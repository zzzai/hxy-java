-- HXY: 门店商品映射权限菜单（API 权限）
SET NAMES utf8mb4;

SET @tenant_id := 1;
SET @admin_role_id := 1;
SET @operator_role_id := 2;
SET @product_parent_id := 2000;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '门店商品映射', '', 2, 3, @product_parent_id, 'store-product', '#',
       '', '', 1, b'0', b'1', b'1', '1', '1', b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu WHERE parent_id = @product_parent_id AND path = 'store-product' AND deleted = 0
);

SET @store_product_menu_id := (
    SELECT id FROM system_menu WHERE parent_id = @product_parent_id AND path = 'store-product' AND deleted = 0
    ORDER BY id DESC LIMIT 1
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status,
                         visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @store_product_menu_id, '', '', '', '', 0,
       b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '门店SPU查询' AS name, 'product:store-spu:query' AS permission, 1 AS sort
         UNION ALL SELECT '门店SPU创建', 'product:store-spu:create', 2
         UNION ALL SELECT '门店SPU更新', 'product:store-spu:update', 3
         UNION ALL SELECT '门店SPU删除', 'product:store-spu:delete', 4
         UNION ALL SELECT '门店SKU查询', 'product:store-sku:query', 5
         UNION ALL SELECT '门店SKU创建', 'product:store-sku:create', 6
         UNION ALL SELECT '门店SKU更新', 'product:store-sku:update', 7
         UNION ALL SELECT '门店SKU删除', 'product:store-sku:delete', 8
     ) seed
WHERE @store_product_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE parent_id = @store_product_menu_id
      AND permission = seed.permission
      AND deleted = 0
);

INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT role_id, menu_id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id, @store_product_menu_id AS menu_id
         UNION ALL SELECT @operator_role_id, @store_product_menu_id
     ) seed
WHERE seed.menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu srm
    WHERE srm.role_id = seed.role_id
      AND srm.menu_id = seed.menu_id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);

INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT roles.role_id, sm.id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id
         UNION ALL SELECT @operator_role_id
     ) roles
         JOIN system_menu sm
              ON sm.parent_id = @store_product_menu_id
                  AND sm.type = 3
                  AND sm.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM system_role_menu srm
    WHERE srm.role_id = roles.role_id
      AND srm.menu_id = sm.id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);
