SET NAMES utf8mb4;

-- HXY: 预约服务评价运营台账菜单（overlay 页面：mall/booking/review/index）

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

SET @booking_parent_id := IFNULL(@booking_parent_id, 2000);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '预约服务评价运营台账', '', 2, 97, @booking_parent_id, 'booking-review', 'ep:chat-line-square',
       'mall/booking/review/index', 'MallBookingReviewIndex',
       0, b'1', b'1', b'0', '1', '1', b'0'
WHERE @booking_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE parent_id = @booking_parent_id
      AND path = 'booking-review'
      AND deleted = 0
);

SET @booking_review_menu_id := (
    SELECT id FROM system_menu
    WHERE parent_id = @booking_parent_id
      AND path = 'booking-review'
      AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @booking_review_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '预约服务评价查询' AS name, 'booking:review:query' AS permission, 1 AS sort
         UNION ALL
         SELECT '预约服务评价处理', 'booking:review:update', 2
     ) seed
WHERE @booking_review_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu sm
    WHERE sm.parent_id = @booking_review_menu_id
      AND sm.permission = seed.permission
      AND sm.deleted = 0
);

INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT role_seed.role_id, menu_seed.menu_id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id
         UNION ALL
         SELECT @operator_role_id
     ) role_seed
         JOIN (
    SELECT @booking_review_menu_id AS menu_id
    UNION ALL
    SELECT id FROM system_menu
    WHERE parent_id = @booking_review_menu_id
      AND deleted = 0
) menu_seed
              ON menu_seed.menu_id IS NOT NULL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role_menu srm
    WHERE srm.role_id = role_seed.role_id
      AND srm.menu_id = menu_seed.menu_id
      AND srm.tenant_id = @tenant_id
      AND srm.deleted = 0
);
