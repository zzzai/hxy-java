SET NAMES utf8mb4;

-- HXY: 商品模板与 SKU 自动生成联调页面菜单

SET @tenant_id := 1;
SET @admin_role_id := 1;
SET @operator_role_id := 2;
SET @product_parent_id := 2000;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '模板与SKU生成', '', 2, 90, @product_parent_id, 'template-generate', 'ep:operation',
       'mall/product/template/index', 'MallProductTemplateIndex',
       0, b'1', b'1', b'0', '1', '1', b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE parent_id = @product_parent_id
      AND path = 'template-generate'
      AND deleted = 0
);

SET @template_page_menu_id := (
    SELECT id FROM system_menu
    WHERE parent_id = @product_parent_id
      AND path = 'template-generate'
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @template_page_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '模板校验' AS name, 'product:template:validate' AS permission, 1 AS sort
         UNION ALL SELECT 'SKU预览', 'product:template:preview', 2
         UNION ALL SELECT 'SKU提交', 'product:template:commit', 3
     ) seed
WHERE @template_page_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu sm
    WHERE sm.parent_id = @template_page_menu_id
      AND sm.permission = seed.permission
      AND sm.deleted = 0
);

INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT role_id, menu_id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id, @template_page_menu_id AS menu_id
         UNION ALL SELECT @operator_role_id, @template_page_menu_id
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
SELECT roles.role_id, menus.id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id
         UNION ALL SELECT @operator_role_id
     ) roles
         JOIN system_menu menus
              ON menus.parent_id = @template_page_menu_id
                  AND menus.type = 3
                  AND menus.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM system_role_menu srm
    WHERE srm.role_id = roles.role_id
      AND srm.menu_id = menus.id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);
