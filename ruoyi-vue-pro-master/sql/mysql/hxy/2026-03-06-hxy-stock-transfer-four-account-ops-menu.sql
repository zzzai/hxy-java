SET NAMES utf8mb4;

-- HXY: 库存调整单审批 + 跨店调拨审批 + 四账运营看板 菜单与权限（幂等）

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

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '库存调整单审批', '', 2, 49, @store_master_menu_id, 'store-sku-stock-adjust-order', 'ep:box',
       'mall/store/stockAdjustOrder/index', 'MallStoreSkuStockAdjustOrderIndex',
       0, b'1', b'1', b'0', '1', '1', b'0'
WHERE @store_master_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_menu
    WHERE parent_id = @store_master_menu_id
      AND path = 'store-sku-stock-adjust-order'
      AND deleted = 0
);

SET @stock_adjust_menu_id := COALESCE(
        (
            SELECT id
            FROM system_menu
            WHERE parent_id = @store_master_menu_id
              AND path = 'store-sku-stock-adjust-order'
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
        ),
        (
            SELECT id
            FROM system_menu
            WHERE path = 'store-sku-stock-adjust-order'
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
        )
    );

UPDATE system_menu
SET name = '库存调整单审批',
    permission = '',
    type = 2,
    sort = 49,
    parent_id = IFNULL(@store_master_menu_id, parent_id),
    path = 'store-sku-stock-adjust-order',
    icon = 'ep:box',
    component = 'mall/store/stockAdjustOrder/index',
    component_name = 'MallStoreSkuStockAdjustOrderIndex',
    status = 0,
    visible = b'1',
    keep_alive = b'1',
    always_show = b'0',
    updater = '1',
    update_time = NOW()
WHERE id = @stock_adjust_menu_id
  AND deleted = 0;

-- 库存调整单复用既有 product:store-sku:query / product:store-sku:update 权限，不重复创建按钮权限菜单

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '跨店调拨审批', '', 2, 50, @store_master_menu_id, 'store-sku-transfer-order', 'ep:switch',
       'mall/store/transferOrder/index', 'MallStoreSkuTransferOrderIndex',
       0, b'1', b'1', b'0', '1', '1', b'0'
WHERE @store_master_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_menu
    WHERE parent_id = @store_master_menu_id
      AND path = 'store-sku-transfer-order'
      AND deleted = 0
);

SET @transfer_menu_id := COALESCE(
        (
            SELECT id
            FROM system_menu
            WHERE parent_id = @store_master_menu_id
              AND path = 'store-sku-transfer-order'
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
        ),
        (
            SELECT id
            FROM system_menu
            WHERE path = 'store-sku-transfer-order'
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
        )
    );

UPDATE system_menu
SET name = '跨店调拨审批',
    permission = '',
    type = 2,
    sort = 50,
    parent_id = IFNULL(@store_master_menu_id, parent_id),
    path = 'store-sku-transfer-order',
    icon = 'ep:switch',
    component = 'mall/store/transferOrder/index',
    component_name = 'MallStoreSkuTransferOrderIndex',
    status = 0,
    visible = b'1',
    keep_alive = b'1',
    always_show = b'0',
    updater = '1',
    update_time = NOW()
WHERE id = @transfer_menu_id
  AND deleted = 0;

-- 跨店调拨单复用既有 product:store-sku:query / product:store-sku:update 权限，不重复创建按钮权限菜单

INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT role_seed.role_id, menu_seed.menu_id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id
         UNION ALL
         SELECT @operator_role_id
     ) role_seed
         JOIN (
              SELECT @stock_adjust_menu_id AS menu_id
              UNION ALL
              SELECT @transfer_menu_id
          ) menu_seed
              ON menu_seed.menu_id IS NOT NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM system_role_menu srm
    WHERE srm.role_id = role_seed.role_id
      AND srm.menu_id = menu_seed.menu_id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);

SET @booking_parent_id := (
    SELECT id
    FROM system_menu
    WHERE path = 'booking'
      AND type = 1
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);

SET @booking_parent_id := IFNULL(@booking_parent_id, (
    SELECT parent_id
    FROM system_menu
    WHERE path = 'booking-commission-settlement'
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
));

SET @booking_parent_id := IFNULL(@booking_parent_id, 2000);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '四账运营看板', '', 2, 97, @booking_parent_id, 'booking-four-account-reconcile', 'ep:data-analysis',
       'mall/booking/fourAccountReconcile/index', 'MallBookingFourAccountReconcileIndex',
       0, b'1', b'1', b'0', '1', '1', b'0'
WHERE @booking_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_menu
    WHERE parent_id = @booking_parent_id
      AND path = 'booking-four-account-reconcile'
      AND deleted = 0
);

SET @four_account_menu_id := COALESCE(
        (
            SELECT id
            FROM system_menu
            WHERE parent_id = @booking_parent_id
              AND path = 'booking-four-account-reconcile'
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
        ),
        (
            SELECT id
            FROM system_menu
            WHERE path = 'booking-four-account-reconcile'
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
        )
    );

UPDATE system_menu
SET name = '四账运营看板',
    permission = '',
    type = 2,
    sort = 97,
    parent_id = IFNULL(@booking_parent_id, parent_id),
    path = 'booking-four-account-reconcile',
    icon = 'ep:data-analysis',
    component = 'mall/booking/fourAccountReconcile/index',
    component_name = 'MallBookingFourAccountReconcileIndex',
    status = 0,
    visible = b'1',
    keep_alive = b'1',
    always_show = b'0',
    updater = '1',
    update_time = NOW()
WHERE id = @four_account_menu_id
  AND deleted = 0;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @four_account_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '四账对账查询' AS name, 'booking:commission:query' AS permission, 1 AS sort
         UNION ALL
         SELECT '四账手工执行', 'booking:commission:settlement', 2
     ) seed
WHERE @four_account_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu sm
    WHERE sm.parent_id = @four_account_menu_id
      AND sm.permission = seed.permission
      AND sm.deleted = 0
);

INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT role_seed.role_id, @four_account_menu_id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id
         UNION ALL
         SELECT @operator_role_id
     ) role_seed
WHERE @four_account_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_role_menu srm
    WHERE srm.role_id = role_seed.role_id
      AND srm.menu_id = @four_account_menu_id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);

INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT role_seed.role_id, menu_seed.id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id
         UNION ALL
         SELECT @operator_role_id
     ) role_seed
         JOIN system_menu menu_seed
              ON menu_seed.parent_id = @four_account_menu_id
                  AND menu_seed.type = 3
                  AND menu_seed.deleted = 0
WHERE NOT EXISTS (
    SELECT 1
    FROM system_role_menu srm
    WHERE srm.role_id = role_seed.role_id
      AND srm.menu_id = menu_seed.id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);
