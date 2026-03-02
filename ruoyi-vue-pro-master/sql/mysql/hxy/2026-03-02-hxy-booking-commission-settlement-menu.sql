SET NAMES utf8mb4;

-- HXY: 技师佣金结算审批流页面菜单（前端路径 + 角色授权）

SET @tenant_id := 1;
SET @admin_role_id := 1;
SET @operator_role_id := 2;
SET @product_parent_id := 2000;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '技师佣金结算', '', 2, 95, @product_parent_id, 'booking-commission-settlement', 'ep:wallet',
       'mall/booking/commission-settlement/index', 'MallBookingCommissionSettlementIndex',
       0, b'1', b'1', b'0', '1', '1', b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE parent_id = @product_parent_id
      AND path = 'booking-commission-settlement'
      AND deleted = 0
);

SET @booking_commission_settlement_menu_id := (
    SELECT id FROM system_menu
    WHERE parent_id = @product_parent_id
      AND path = 'booking-commission-settlement'
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '佣金通知出站', '', 2, 96, @product_parent_id, 'booking-commission-outbox', 'ep:bell',
       'mall/booking/commission-settlement/outbox/index', 'MallBookingCommissionSettlementOutboxIndex',
       0, b'0', b'1', b'0', '1', '1', b'0'
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

INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT role_id, menu_id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id, @booking_commission_settlement_menu_id AS menu_id
         UNION ALL SELECT @operator_role_id, @booking_commission_settlement_menu_id
         UNION ALL SELECT @admin_role_id, @booking_commission_outbox_menu_id
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
