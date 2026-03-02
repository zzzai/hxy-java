SET NAMES utf8mb4;

SET @tenant_id := 1;
SET @admin_role_id := 1;
SET @operator_role_id := 2;
SET @store_root_parent_id := 0;

-- 优先复用已有 store-master 菜单，迁移为一级菜单；不存在则创建
SET @store_master_menu_id := (
    SELECT id FROM system_menu
    WHERE path IN ('store-master', '/mall/product/store-master') AND deleted = 0
    ORDER BY id DESC LIMIT 1
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '门店管理', '', 1, 60, @store_root_parent_id, '/mall/product/store-master', 'ep:office-building',
       '', '', 0, b'1', b'1', b'1', '1', '1', b'0'
WHERE @store_master_menu_id IS NULL;

SET @store_master_menu_id := (
    SELECT id FROM system_menu
    WHERE path IN ('store-master', '/mall/product/store-master') AND deleted = 0
    ORDER BY id DESC LIMIT 1
);

UPDATE system_menu
SET name = '门店管理',
    type = 1,
    sort = 60,
    parent_id = @store_root_parent_id,
    path = '/mall/product/store-master',
    icon = 'ep:office-building',
    status = 0,
    visible = b'1',
    keep_alive = b'1',
    always_show = b'1',
    updater = '1'
WHERE id = @store_master_menu_id;

-- 二级：门店主数据页面
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '门店档案', '', 2, 10, @store_master_menu_id, 'store-list', 'ep:shop',
       'mall/store/index', 'MallStoreIndex', 0, b'1', b'1', b'0', '1', '1', b'0'
WHERE @store_master_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE parent_id = @store_master_menu_id AND path = 'store-list' AND deleted = 0
);

SET @store_page_menu_id := (
    SELECT id FROM system_menu WHERE parent_id = @store_master_menu_id AND path = 'store-list' AND deleted = 0
    ORDER BY id DESC LIMIT 1
);

-- 二级：门店分类页面
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '门店分类', '', 2, 20, @store_master_menu_id, 'store-category', 'ep:collection-tag',
       'mall/store/category/index', 'MallStoreCategoryIndex', 0, b'1', b'1', b'0', '1', '1', b'0'
WHERE @store_master_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE parent_id = @store_master_menu_id AND path = 'store-category' AND deleted = 0
);

SET @store_category_page_menu_id := (
    SELECT id FROM system_menu WHERE parent_id = @store_master_menu_id AND path = 'store-category' AND deleted = 0
    ORDER BY id DESC LIMIT 1
);

-- 二级：门店标签页面
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '门店标签', '', 2, 30, @store_master_menu_id, 'store-tag', 'ep:price-tag',
       'mall/store/tag/index', 'MallStoreTagIndex', 0, b'1', b'1', b'0', '1', '1', b'0'
WHERE @store_master_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE parent_id = @store_master_menu_id AND path = 'store-tag' AND deleted = 0
);

SET @store_tag_page_menu_id := (
    SELECT id FROM system_menu WHERE parent_id = @store_master_menu_id AND path = 'store-tag' AND deleted = 0
    ORDER BY id DESC LIMIT 1
);

-- 按钮权限：门店档案
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @store_page_menu_id, '', '', '', '', 0,
       b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '门店查询' AS name, 'product:store:query' AS permission, 1 AS sort
         UNION ALL SELECT '门店创建', 'product:store:create', 2
         UNION ALL SELECT '门店更新', 'product:store:update', 3
         UNION ALL SELECT '门店删除', 'product:store:delete', 4
     ) seed
WHERE @store_page_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu sm
    WHERE sm.parent_id = @store_page_menu_id AND sm.permission = seed.permission AND sm.deleted = 0
);

-- 按钮权限：门店分类
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @store_category_page_menu_id, '', '', '', '', 0,
       b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '门店分类查询' AS name, 'product:store-category:query' AS permission, 1 AS sort
         UNION ALL SELECT '门店分类创建', 'product:store-category:create', 2
         UNION ALL SELECT '门店分类更新', 'product:store-category:update', 3
         UNION ALL SELECT '门店分类删除', 'product:store-category:delete', 4
     ) seed
WHERE @store_category_page_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu sm
    WHERE sm.parent_id = @store_category_page_menu_id AND sm.permission = seed.permission AND sm.deleted = 0
);

-- 按钮权限：门店标签
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @store_tag_page_menu_id, '', '', '', '', 0,
       b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '门店标签查询' AS name, 'product:store-tag:query' AS permission, 1 AS sort
         UNION ALL SELECT '门店标签创建', 'product:store-tag:create', 2
         UNION ALL SELECT '门店标签更新', 'product:store-tag:update', 3
         UNION ALL SELECT '门店标签删除', 'product:store-tag:delete', 4
     ) seed
WHERE @store_tag_page_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu sm
    WHERE sm.parent_id = @store_tag_page_menu_id AND sm.permission = seed.permission AND sm.deleted = 0
);

-- 角色授权（admin/operator）
INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT roles.role_id, menus.id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id
         UNION ALL SELECT @operator_role_id
     ) roles
         JOIN system_menu menus
              ON menus.deleted = b'0'
                  AND (
                     menus.id = @store_master_menu_id
                     OR menus.parent_id IN (@store_master_menu_id, @store_page_menu_id, @store_category_page_menu_id, @store_tag_page_menu_id)
                  )
WHERE menus.id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu srm
    WHERE srm.role_id = roles.role_id
      AND srm.menu_id = menus.id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);
