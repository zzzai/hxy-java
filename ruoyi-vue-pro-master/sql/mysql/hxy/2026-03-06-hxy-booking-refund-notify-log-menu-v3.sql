SET NAMES utf8mb4;

-- HXY V3: 退款回调日志管理菜单权限补齐（自动重放触发 + 批次历史查询）
-- 仅补按钮权限，不重复创建页面菜单

SET @tenant_id := 1;
SET @admin_role_id := 1;
SET @operator_role_id := 2;

SET @refund_notify_log_menu_id := (
    SELECT id FROM system_menu
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
         SELECT '自动补偿重放触发' AS name, 'booking:refund-notify-log:replay-due' AS permission, 3 AS sort
         UNION ALL
         SELECT '重放批次历史查询' AS name, 'booking:refund-notify-log:replay-run-log:query' AS permission, 4 AS sort
     ) seed
WHERE @refund_notify_log_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu sm
    WHERE sm.parent_id = @refund_notify_log_menu_id
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
              ON menu_seed.parent_id = @refund_notify_log_menu_id
                  AND menu_seed.type = 3
                  AND menu_seed.deleted = 0
WHERE @refund_notify_log_menu_id IS NOT NULL
  AND menu_seed.permission IN ('booking:refund-notify-log:replay-due', 'booking:refund-notify-log:replay-run-log:query')
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu srm
    WHERE srm.role_id = role_seed.role_id
      AND srm.menu_id = menu_seed.id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);
