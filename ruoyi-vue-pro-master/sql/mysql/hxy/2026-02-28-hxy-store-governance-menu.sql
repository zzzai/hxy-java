SET @tenant_id := 1;
SET @admin_role_id := 1;
SET @operator_role_id := 2;

SET @store_master_menu_id := (
    SELECT id FROM system_menu WHERE path = 'store-master' AND deleted = 0 ORDER BY id DESC LIMIT 1
);

SET @store_page_menu_id := (
    SELECT id FROM system_menu WHERE parent_id = @store_master_menu_id AND path = 'store-list' AND deleted = 0 ORDER BY id DESC LIMIT 1
);

-- 二级：标签组页面
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '标签组规则', '', 2, 40, @store_master_menu_id, 'store-tag-group', 'ep:price-tag',
       'mall/store/tag-group/index', 'MallStoreTagGroupIndex', 0, b'1', b'1', b'0', '1', '1', b'0'
WHERE @store_master_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE parent_id = @store_master_menu_id AND path = 'store-tag-group' AND deleted = 0
);

SET @store_tag_group_page_menu_id := (
    SELECT id FROM system_menu WHERE parent_id = @store_master_menu_id AND path = 'store-tag-group' AND deleted = 0
    ORDER BY id DESC LIMIT 1
);

-- 按钮权限：标签组
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @store_tag_group_page_menu_id, '', '', '', '', 0,
       b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '标签组查询' AS name, 'product:store-tag-group:query' AS permission, 1 AS sort
         UNION ALL SELECT '标签组创建', 'product:store-tag-group:create', 2
         UNION ALL SELECT '标签组更新', 'product:store-tag-group:update', 3
         UNION ALL SELECT '标签组删除', 'product:store-tag-group:delete', 4
     ) seed
WHERE @store_tag_group_page_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu sm
    WHERE sm.parent_id = @store_tag_group_page_menu_id AND sm.permission = seed.permission AND sm.deleted = 0
);

-- 按钮权限：门店上线门禁与批量更新
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @store_page_menu_id, '', '', '', '', 0,
       b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '门店上线门禁检查' AS name, 'product:store:check-launch-readiness' AS permission, 10 AS sort
         UNION ALL SELECT '门店批量更新分类', 'product:store:batch-category', 11
         UNION ALL SELECT '门店批量更新标签', 'product:store:batch-tags', 12
         UNION ALL SELECT '门店批量更新生命周期', 'product:store:batch-lifecycle', 13
     ) seed
WHERE @store_page_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu sm
    WHERE sm.parent_id = @store_page_menu_id AND sm.permission = seed.permission AND sm.deleted = 0
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
                     menus.id = @store_tag_group_page_menu_id
                     OR menus.parent_id = @store_tag_group_page_menu_id
                     OR (menus.parent_id = @store_page_menu_id
                         AND menus.permission IN (
                              'product:store:check-launch-readiness',
                              'product:store:batch-category',
                              'product:store:batch-tags',
                              'product:store:batch-lifecycle'
                         ))
                  )
WHERE menus.id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu srm
    WHERE srm.role_id = roles.role_id
      AND srm.menu_id = menus.id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);
