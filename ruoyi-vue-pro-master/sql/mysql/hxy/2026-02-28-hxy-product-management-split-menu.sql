-- HXY: 商品后台拆分为两套入口（服务项目 / 实物商品）
-- 目标：
-- 1) 固定挂载到商品中心（menu_id=2000）
-- 2) 按 permission 分离后端接口权限
-- 3) 下线旧 product:spu:* 通用入口，避免混管

SET NAMES utf8mb4;

SET @tenant_id := 1;
SET @admin_role_id := 1;
SET @operator_role_id := 2;
SET @product_parent_id := 2000;

-- 兼容旧脚本：goods-spu 改名为 physical-spu
UPDATE system_menu
SET path = 'physical-spu',
    name = '实物商品管理',
    updater = '1',
    update_time = NOW()
WHERE parent_id = @product_parent_id
  AND path = 'goods-spu'
  AND deleted = 0;

-- 若 service-spu 已存在，统一命名
UPDATE system_menu
SET name = '服务项目管理',
    component = 'mall/product/spu/index',
    updater = '1',
    update_time = NOW()
WHERE parent_id = @product_parent_id
  AND path = 'service-spu'
  AND deleted = 0;

-- 服务项目目录菜单
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '服务项目管理', '', 2, 1, @product_parent_id, 'service-spu', '#',
       'mall/product/spu/index', '', 0, b'1', b'1', b'1', '1', '1', b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu WHERE parent_id = @product_parent_id AND path = 'service-spu' AND deleted = 0
);

-- 实物商品目录菜单
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '实物商品管理', '', 2, 2, @product_parent_id, 'physical-spu', '#',
       'mall/product/spu/index', '', 0, b'1', b'1', b'1', '1', '1', b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu WHERE parent_id = @product_parent_id AND path = 'physical-spu' AND deleted = 0
);

SET @service_menu_id := (
    SELECT id FROM system_menu WHERE parent_id = @product_parent_id AND path = 'service-spu' AND deleted = 0
    ORDER BY id DESC LIMIT 1
);
SET @physical_menu_id := (
    SELECT id FROM system_menu WHERE parent_id = @product_parent_id AND path = 'physical-spu' AND deleted = 0
    ORDER BY id DESC LIMIT 1
);

-- 规范目录状态
UPDATE system_menu
SET visible = b'1',
    status = 0,
    updater = '1',
    update_time = NOW()
WHERE id IN (@service_menu_id, @physical_menu_id);

-- 服务项目按钮权限
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '服务项目查询', 'product:service-spu:query', 3, 1, @service_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
WHERE @service_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_menu WHERE parent_id = @service_menu_id
        AND permission = 'product:service-spu:query' AND deleted = 0
  );
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '服务项目创建', 'product:service-spu:create', 3, 2, @service_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
WHERE @service_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_menu WHERE parent_id = @service_menu_id
        AND permission = 'product:service-spu:create' AND deleted = 0
  );
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '服务项目更新', 'product:service-spu:update', 3, 3, @service_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
WHERE @service_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_menu WHERE parent_id = @service_menu_id
        AND permission = 'product:service-spu:update' AND deleted = 0
  );
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '服务项目删除', 'product:service-spu:delete', 3, 4, @service_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
WHERE @service_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_menu WHERE parent_id = @service_menu_id
        AND permission = 'product:service-spu:delete' AND deleted = 0
  );

-- 实物商品按钮权限
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '实物商品查询', 'product:physical-spu:query', 3, 1, @physical_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
WHERE @physical_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_menu WHERE parent_id = @physical_menu_id
        AND permission = 'product:physical-spu:query' AND deleted = 0
  );
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '实物商品创建', 'product:physical-spu:create', 3, 2, @physical_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
WHERE @physical_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_menu WHERE parent_id = @physical_menu_id
        AND permission = 'product:physical-spu:create' AND deleted = 0
  );
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '实物商品更新', 'product:physical-spu:update', 3, 3, @physical_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
WHERE @physical_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_menu WHERE parent_id = @physical_menu_id
        AND permission = 'product:physical-spu:update' AND deleted = 0
  );
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '实物商品删除', 'product:physical-spu:delete', 3, 4, @physical_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
WHERE @physical_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_menu WHERE parent_id = @physical_menu_id
        AND permission = 'product:physical-spu:delete' AND deleted = 0
  );

-- 强制按钮挂载到规范目录（清理历史错误 parent_id）
UPDATE system_menu
SET parent_id = @service_menu_id,
    updater = '1',
    update_time = NOW()
WHERE permission LIKE 'product:service-spu:%'
  AND deleted = 0
  AND @service_menu_id IS NOT NULL;

UPDATE system_menu
SET parent_id = @physical_menu_id,
    updater = '1',
    update_time = NOW()
WHERE permission LIKE 'product:physical-spu:%'
  AND deleted = 0
  AND @physical_menu_id IS NOT NULL;

-- 去重：每个权限仅保留最早一条菜单
UPDATE system_menu sm
JOIN (
    SELECT permission, MIN(id) AS keep_id
    FROM system_menu
    WHERE permission IN (
        'product:service-spu:query',
        'product:service-spu:create',
        'product:service-spu:update',
        'product:service-spu:delete',
        'product:physical-spu:query',
        'product:physical-spu:create',
        'product:physical-spu:update',
        'product:physical-spu:delete'
    )
      AND deleted = 0
    GROUP BY permission
    HAVING COUNT(*) > 1
) dup ON dup.permission = sm.permission
SET sm.deleted = b'1',
    sm.visible = b'0',
    sm.status = 1,
    sm.updater = '1',
    sm.update_time = NOW()
WHERE sm.id <> dup.keep_id;

-- 角色菜单关系同步清理（软删除已去重掉的按钮）
UPDATE system_role_menu srm
JOIN system_menu sm ON sm.id = srm.menu_id
SET srm.deleted = b'1',
    srm.updater = '1',
    srm.update_time = NOW()
WHERE sm.permission IN (
        'product:service-spu:query',
        'product:service-spu:create',
        'product:service-spu:update',
        'product:service-spu:delete',
        'product:physical-spu:query',
        'product:physical-spu:create',
        'product:physical-spu:update',
        'product:physical-spu:delete'
    )
  AND sm.deleted = b'1'
  AND srm.deleted = b'0';

-- 角色授权（管理员 + 运营）
INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT role_id, menu_id, '1', '1', b'0', @tenant_id
FROM (
    SELECT @admin_role_id AS role_id, @service_menu_id AS menu_id
    UNION ALL SELECT @operator_role_id, @service_menu_id
    UNION ALL SELECT @admin_role_id, @physical_menu_id
    UNION ALL SELECT @operator_role_id, @physical_menu_id
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
SELECT role_id, id, '1', '1', b'0', @tenant_id
FROM (
    SELECT @admin_role_id AS role_id
    UNION ALL SELECT @operator_role_id
) roles
JOIN system_menu sm
  ON sm.parent_id IN (@service_menu_id, @physical_menu_id)
 AND sm.type = 3
 AND sm.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM system_role_menu srm
    WHERE srm.role_id = roles.role_id
      AND srm.menu_id = sm.id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);

-- 下线旧通用商品入口
UPDATE system_menu
SET visible = b'0',
    status = 1,
    updater = '1',
    update_time = NOW()
WHERE deleted = 0
  AND (
      id = 2014
      OR parent_id = 2014
      OR permission LIKE 'product:spu:%'
  );

-- 隐藏错误挂载到其它父节点的 service/physical 目录（保留数据，不删除）
UPDATE system_menu
SET visible = b'0',
    status = 1,
    updater = '1',
    update_time = NOW()
WHERE deleted = 0
  AND type = 2
  AND path IN ('service-spu', 'physical-spu')
  AND parent_id <> @product_parent_id;
