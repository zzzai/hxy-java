SET NAMES utf8mb4;

-- HXY: 技师佣金结算通知出站页面菜单（前端路径 + 权限）

SET @tenant_id := 1;
SET @admin_role_id := 1;
SET @operator_role_id := 2;
SET @product_parent_id := 2000;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '佣金通知出站', '', 2, 96, @product_parent_id, 'booking-commission-outbox', 'ep:bell',
       'mall/booking/commission-settlement/outbox/index', 'MallBookingCommissionSettlementOutboxIndex',
       0, b'1', b'1', b'0', '1', '1', b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE parent_id = @product_parent_id
      AND path = 'booking-commission-outbox'
      AND deleted = 0
);

SET @booking_commission_outbox_menu_id := (
    SELECT id FROM system_menu
    WHERE parent_id = @product_parent_id
      AND path = 'booking-commission-outbox'
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @booking_commission_outbox_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '佣金通知查询' AS name, 'booking:commission:query' AS permission, 1 AS sort
         UNION ALL SELECT '佣金通知重试', 'booking:commission:settlement', 2
     ) seed
WHERE @booking_commission_outbox_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu sm
    WHERE sm.parent_id = @booking_commission_outbox_menu_id
      AND sm.permission = seed.permission
      AND sm.deleted = 0
);

INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT role_id, menu_id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id, @booking_commission_outbox_menu_id AS menu_id
         UNION ALL SELECT @operator_role_id, @booking_commission_outbox_menu_id
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
              ON menus.parent_id = @booking_commission_outbox_menu_id
                  AND menus.type = 3
                  AND menus.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM system_role_menu srm
    WHERE srm.role_id = roles.role_id
      AND srm.menu_id = menus.id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);
