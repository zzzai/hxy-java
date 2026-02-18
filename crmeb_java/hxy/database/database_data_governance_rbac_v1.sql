-- 数据治理 RBAC 补丁（幂等）
-- 目标：补齐 admin:data:governance:* 权限，避免管理端接口 403

SET @ops_root_id := (
    SELECT id FROM eb_system_menu
    WHERE name = '运营' AND menu_type = 'M' AND is_delte = 0
    ORDER BY id ASC
    LIMIT 1
);
SET @ops_root_id := IFNULL(@ops_root_id, 0);

INSERT INTO eb_system_menu
    (pid, name, icon, perms, component, menu_type, sort, is_show, is_delte, create_time, update_time)
SELECT
    @ops_root_id, '数据治理', 'md-information', '', '/operation/privacy/governance', 'M', 88, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM eb_system_menu
    WHERE name = '数据治理' AND menu_type = 'M' AND is_delte = 0
);

SET @governance_menu_id := (
    SELECT id FROM eb_system_menu
    WHERE name = '数据治理' AND menu_type = 'M' AND is_delte = 0
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO eb_system_menu
    (pid, name, icon, perms, component, menu_type, sort, is_show, is_delte, create_time, update_time)
SELECT
    @governance_menu_id, '数据治理控制台', 'md-grid', '', '/privacy/governance/list', 'C', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM eb_system_menu
    WHERE name = '数据治理控制台' AND menu_type = 'C' AND is_delte = 0
);

SET @governance_console_id := (
    SELECT id FROM eb_system_menu
    WHERE name = '数据治理控制台' AND menu_type = 'C' AND is_delte = 0
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO eb_system_menu
    (pid, name, icon, perms, component, menu_type, sort, is_show, is_delte, create_time, update_time)
SELECT
    @governance_console_id, '字段清单分页列表', '', 'admin:data:governance:field:list', '', 'A', 10, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:data:governance:field:list' AND is_delte = 0);

INSERT INTO eb_system_menu
    (pid, name, icon, perms, component, menu_type, sort, is_show, is_delte, create_time, update_time)
SELECT
    @governance_console_id, '授权记录分页列表', '', 'admin:data:governance:consent:list', '', 'A', 20, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:data:governance:consent:list' AND is_delte = 0);

INSERT INTO eb_system_menu
    (pid, name, icon, perms, component, menu_type, sort, is_show, is_delte, create_time, update_time)
SELECT
    @governance_console_id, '访问工单创建', '', 'admin:data:governance:access-ticket:create', '', 'A', 30, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:data:governance:access-ticket:create' AND is_delte = 0);

INSERT INTO eb_system_menu
    (pid, name, icon, perms, component, menu_type, sort, is_show, is_delte, create_time, update_time)
SELECT
    @governance_console_id, '访问工单审批', '', 'admin:data:governance:access-ticket:approve', '', 'A', 31, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:data:governance:access-ticket:approve' AND is_delte = 0);

INSERT INTO eb_system_menu
    (pid, name, icon, perms, component, menu_type, sort, is_show, is_delte, create_time, update_time)
SELECT
    @governance_console_id, '访问工单驳回', '', 'admin:data:governance:access-ticket:reject', '', 'A', 32, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:data:governance:access-ticket:reject' AND is_delte = 0);

INSERT INTO eb_system_menu
    (pid, name, icon, perms, component, menu_type, sort, is_show, is_delte, create_time, update_time)
SELECT
    @governance_console_id, '访问工单关闭', '', 'admin:data:governance:access-ticket:close', '', 'A', 33, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:data:governance:access-ticket:close' AND is_delte = 0);

INSERT INTO eb_system_menu
    (pid, name, icon, perms, component, menu_type, sort, is_show, is_delte, create_time, update_time)
SELECT
    @governance_console_id, '访问工单分页列表', '', 'admin:data:governance:access-ticket:list', '', 'A', 34, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:data:governance:access-ticket:list' AND is_delte = 0);

INSERT INTO eb_system_menu
    (pid, name, icon, perms, component, menu_type, sort, is_show, is_delte, create_time, update_time)
SELECT
    @governance_console_id, '删除请求分页列表', '', 'admin:data:governance:deletion:list', '', 'A', 40, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:data:governance:deletion:list' AND is_delte = 0);

INSERT INTO eb_system_menu
    (pid, name, icon, perms, component, menu_type, sort, is_show, is_delte, create_time, update_time)
SELECT
    @governance_console_id, '标签策略分页列表', '', 'admin:data:governance:label-policy:list', '', 'A', 50, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:data:governance:label-policy:list' AND is_delte = 0);

INSERT INTO eb_system_menu
    (pid, name, icon, perms, component, menu_type, sort, is_show, is_delte, create_time, update_time)
SELECT
    @governance_console_id, '标签策略状态变更', '', 'admin:data:governance:label-policy:update-status', '', 'A', 51, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:data:governance:label-policy:update-status' AND is_delte = 0);

-- 绑定超级管理员角色（rid=1），保持菜单树一致
INSERT IGNORE INTO eb_system_role_menu (rid, menu_id)
SELECT 1, id
FROM eb_system_menu
WHERE is_delte = 0
  AND (
      name IN ('数据治理', '数据治理控制台')
      OR perms LIKE 'admin:data:governance:%'
  );
