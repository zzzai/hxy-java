SET NAMES utf8mb4;

-- HXY: 退款回调日志管理菜单（overlay 页面：mall/booking/refundNotifyLog/index）

SET @tenant_id := 1;
SET @admin_role_id := 1;
SET @operator_role_id := 2;

SET @booking_parent_id := (
    SELECT id FROM system_menu
    WHERE path = 'booking'
      AND type = 1
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);

SET @booking_parent_id := IFNULL(@booking_parent_id, (
    SELECT parent_id FROM system_menu
    WHERE path = 'booking-four-account-reconcile'
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
));

SET @booking_parent_id := IFNULL(@booking_parent_id, 2000);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '退款回调日志管理', '', 2, 98, @booking_parent_id, 'booking-refund-notify-log', 'ep:warning-filled',
       'mall/booking/refundNotifyLog/index', 'MallBookingRefundNotifyLogIndex',
       0, b'1', b'1', b'0', '1', '1', b'0'
WHERE @booking_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE parent_id = @booking_parent_id
      AND path = 'booking-refund-notify-log'
      AND deleted = 0
);

SET @refund_notify_log_menu_id := (
    SELECT id FROM system_menu
    WHERE parent_id = @booking_parent_id
      AND path = 'booking-refund-notify-log'
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @refund_notify_log_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '退款回调日志查询' AS name, 'booking:refund-notify-log:query' AS permission, 1 AS sort
         UNION ALL
         SELECT '退款回调日志重放' AS name, 'booking:refund-notify-log:replay' AS permission, 2 AS sort
     ) seed
WHERE @refund_notify_log_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu sm
    WHERE sm.parent_id = @refund_notify_log_menu_id
      AND sm.permission = seed.permission
      AND sm.deleted = 0
);

INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT role_seed.role_id, @refund_notify_log_menu_id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id
         UNION ALL
         SELECT @operator_role_id
     ) role_seed
WHERE @refund_notify_log_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu srm
    WHERE srm.role_id = role_seed.role_id
      AND srm.menu_id = @refund_notify_log_menu_id
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
              ON menu_seed.parent_id = @refund_notify_log_menu_id
                  AND menu_seed.type = 3
                  AND menu_seed.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM system_role_menu srm
    WHERE srm.role_id = role_seed.role_id
      AND srm.menu_id = menu_seed.id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);
