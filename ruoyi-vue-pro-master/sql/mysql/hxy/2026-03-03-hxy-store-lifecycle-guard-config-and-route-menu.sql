SET NAMES utf8mb4;

-- HXY: 门店生命周期守卫模式配置（WARN/BLOCK）+ 售后工单 SLA 路由菜单

SET @tenant_id := 1;
SET @admin_role_id := 1;
SET @operator_role_id := 2;

-- =========================
-- 1) lifecycle guard 配置种子
-- =========================
INSERT INTO infra_config (category, type, name, config_key, value, visible, remark, creator, updater, deleted)
SELECT 'mall.store.lifecycle.guard', 2, seed.name, seed.config_key, 'WARN', b'0',
       '门店生命周期守卫模式：WARN 仅审计不阻塞，BLOCK 阻塞流转', '1', '1', b'0'
FROM (
         SELECT '门店守卫-商品映射模式' AS name, 'hxy.store.lifecycle.guard.mapping.mode' AS config_key
         UNION ALL SELECT '门店守卫-正库存模式', 'hxy.store.lifecycle.guard.stock.mode'
         UNION ALL SELECT '门店守卫-库存流水模式', 'hxy.store.lifecycle.guard.stock-flow.mode'
         UNION ALL SELECT '门店守卫-未结订单模式', 'hxy.store.lifecycle.guard.pending-order.mode'
         UNION ALL SELECT '门店守卫-在途售后模式', 'hxy.store.lifecycle.guard.inflight-ticket.mode'
     ) seed
WHERE NOT EXISTS (
    SELECT 1 FROM infra_config c
    WHERE c.config_key = seed.config_key
      AND c.deleted = 0
);

-- =========================
-- 2) 售后工单 SLA 路由页面菜单
-- =========================
SET @trade_menu_id := (
    SELECT id FROM system_menu
    WHERE path = 'trade' AND type = 1 AND deleted = 0
    ORDER BY id DESC LIMIT 1
);

SET @after_sale_menu_id := (
    SELECT id FROM system_menu
    WHERE parent_id = @trade_menu_id
      AND path = 'after-sale'
      AND deleted = 0
    ORDER BY id DESC LIMIT 1
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT '工单 SLA 路由', '', 2, 25, @after_sale_menu_id, 'review-ticket-route', 'ep:timer',
       'mall/trade/reviewTicketRoute/index', 'MallTradeReviewTicketRouteIndex',
       0, b'1', b'1', b'0', '1', '1', b'0'
WHERE @after_sale_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE parent_id = @after_sale_menu_id
      AND path = 'review-ticket-route'
      AND deleted = 0
);

SET @review_ticket_route_menu_id := (
    SELECT id FROM system_menu
    WHERE parent_id = @after_sale_menu_id
      AND path = 'review-ticket-route'
      AND deleted = 0
    ORDER BY id DESC LIMIT 1
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT seed.name, seed.permission, 3, seed.sort, @review_ticket_route_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', '1', '1', b'0'
FROM (
         SELECT '工单SLA路由查询' AS name, 'trade:after-sale:query' AS permission, 1 AS sort
         UNION ALL SELECT '工单SLA路由维护', 'trade:after-sale:refund', 2
     ) seed
WHERE @review_ticket_route_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu sm
    WHERE sm.parent_id = @review_ticket_route_menu_id
      AND sm.permission = seed.permission
      AND sm.deleted = 0
);

INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT role_seed.role_id, menu_seed.menu_id, '1', '1', b'0', @tenant_id
FROM (
         SELECT @admin_role_id AS role_id
         UNION ALL SELECT @operator_role_id
     ) role_seed
         JOIN (
    SELECT @review_ticket_route_menu_id AS menu_id
    UNION ALL
    SELECT id FROM system_menu
    WHERE parent_id = @review_ticket_route_menu_id
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
