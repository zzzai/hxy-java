SET NAMES utf8mb4;

SET @tenant_id := 1;
SET @admin_role_id := 1;
SET @operator_role_id := 2;

SET @store_master_menu_id := (
    SELECT id
    FROM system_menu
    WHERE path IN ('store-master', '/mall/product/store-master')
      AND type = 1
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);

-- 生命周期批次台账
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT 'Lifecycle Batch Log', '', 2, 45, @store_master_menu_id, 'store-lifecycle-batch-log', 'ep:document',
       'mall/store/lifecycleBatchLog/index', 'MallStoreLifecycleBatchLogIndex',
       0, b'1', b'1', b'0', '1', '1', b'0'
WHERE @store_master_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_menu
    WHERE parent_id = @store_master_menu_id
      AND path = 'store-lifecycle-batch-log'
      AND deleted = 0
);

-- 生命周期复核历史
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT 'Lifecycle Recheck Log', '', 2, 46, @store_master_menu_id, 'store-lifecycle-recheck-log', 'ep:clock',
       'mall/store/lifecycleRecheckLog/index', 'MallStoreLifecycleRecheckLogIndex',
       0, b'1', b'1', b'0', '1', '1', b'0'
WHERE @store_master_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_menu
    WHERE parent_id = @store_master_menu_id
      AND path = 'store-lifecycle-recheck-log'
      AND deleted = 0
);

SET @store_lifecycle_batch_log_menu_id := (
    SELECT id
    FROM system_menu
    WHERE parent_id = @store_master_menu_id
      AND path = 'store-lifecycle-batch-log'
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);

SET @store_lifecycle_recheck_log_menu_id := (
    SELECT id
    FROM system_menu
    WHERE parent_id = @store_master_menu_id
      AND path = 'store-lifecycle-recheck-log'
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT role_seed.role_id, menu_seed.menu_id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id
         UNION ALL
         SELECT @operator_role_id
     ) role_seed
         JOIN (
    SELECT @store_lifecycle_batch_log_menu_id AS menu_id
    UNION ALL
    SELECT @store_lifecycle_recheck_log_menu_id
) menu_seed ON menu_seed.menu_id IS NOT NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM system_role_menu srm
    WHERE srm.role_id = role_seed.role_id
      AND srm.menu_id = menu_seed.menu_id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);
