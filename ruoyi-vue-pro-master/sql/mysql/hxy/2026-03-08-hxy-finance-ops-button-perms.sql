SET NAMES utf8mb4;

-- HXY: 财务运营联调收口按钮权限补齐（幂等）
-- 仅补按钮权限，不重复创建菜单

SET @tenant_id := 1;
SET @admin_role_id := 1;
SET @operator_role_id := 2;

SET @refund_notify_log_menu_id := (
    SELECT id
    FROM system_menu
    WHERE path = 'booking-refund-notify-log'
      AND type = 2
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @refund_notify_log_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '重放批次同步工单' AS name, 'booking:refund-notify-log:replay-run-log:sync-tickets' AS permission, 5 AS sort
     ) seed
WHERE @refund_notify_log_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_menu sm
    WHERE sm.parent_id = @refund_notify_log_menu_id
      AND sm.permission = seed.permission
      AND sm.deleted = 0
);

SET @four_account_menu_id := (
    SELECT id
    FROM system_menu
    WHERE path = 'booking-four-account-reconcile'
      AND type = 2
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @four_account_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '退款审计同步工单' AS name, 'booking:commission:refund-audit:sync-tickets' AS permission, 3 AS sort
     ) seed
WHERE @four_account_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_menu sm
    WHERE sm.parent_id = @four_account_menu_id
      AND sm.permission = seed.permission
      AND sm.deleted = 0
);

INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT role_seed.role_id, menu_seed.id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id
         UNION ALL
         SELECT @operator_role_id
     ) role_seed
         JOIN system_menu menu_seed
              ON menu_seed.type = 3
                  AND menu_seed.deleted = 0
                  AND (
                      (menu_seed.parent_id = @refund_notify_log_menu_id
                          AND menu_seed.permission = 'booking:refund-notify-log:replay-run-log:sync-tickets')
                      OR
                      (menu_seed.parent_id = @four_account_menu_id
                          AND menu_seed.permission = 'booking:commission:refund-audit:sync-tickets')
                      )
WHERE NOT EXISTS (
    SELECT 1
    FROM system_role_menu srm
    WHERE srm.role_id = role_seed.role_id
      AND srm.menu_id = menu_seed.id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);
