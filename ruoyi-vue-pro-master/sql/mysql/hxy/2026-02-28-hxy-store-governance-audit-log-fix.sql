SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- HXY: 门店治理审计日志补丁
-- 背景：门店治理服务在保存标签/标签组/门店信息时会写入 hxy_store_audit_log
-- 若缺表会导致 /admin-api/product/store-tag/save 与 /store-tag-group/save 500

CREATE TABLE IF NOT EXISTS `hxy_store_audit_log`
(
    `id`              bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `domain`          varchar(32) NOT NULL COMMENT '审计域：STORE/CATEGORY/TAG/TAG_GROUP',
    `domain_id`       bigint      NOT NULL COMMENT '审计对象编号',
    `action`          varchar(32) NOT NULL COMMENT '操作：CREATE/UPDATE/DELETE/BATCH/LIFECYCLE',
    `before_snapshot` longtext    NULL COMMENT '变更前快照（JSON）',
    `after_snapshot`  longtext    NULL COMMENT '变更后快照（JSON）',
    `reason`          varchar(255)         DEFAULT '' COMMENT '变更原因',
    `creator`         varchar(64)          DEFAULT '' COMMENT '创建者',
    `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`         varchar(64)          DEFAULT '' COMMENT '更新者',
    `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`       bigint      NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_domain_domain_id` (`domain`, `domain_id`),
    KEY `idx_tenant_create_time` (`tenant_id`, `create_time`)
) COMMENT ='门店治理审计日志';

SET FOREIGN_KEY_CHECKS = 1;
