SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- HXY: 门店治理二阶段结构升级（支持标签组在线治理）
-- 目标：
-- 1) 补齐门店生命周期字段
-- 2) 补齐门店分类层级字段
-- 3) 新增门店标签组表
-- 4) 为门店标签补齐 group_id 并回填历史数据

-- 门店生命周期字段
SET @col_store_lifecycle_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'hxy_store'
      AND column_name = 'lifecycle_status'
);
SET @sql_add_store_lifecycle := IF(@col_store_lifecycle_exists = 0,
                                   'ALTER TABLE `hxy_store` ADD COLUMN `lifecycle_status` tinyint NOT NULL DEFAULT 30 COMMENT ''生命周期状态：10筹备中 20试营业 30营业中 35停业 40闭店'' AFTER `status`',
                                   'SELECT 1');
PREPARE stmt_add_store_lifecycle FROM @sql_add_store_lifecycle;
EXECUTE stmt_add_store_lifecycle;
DEALLOCATE PREPARE stmt_add_store_lifecycle;

UPDATE `hxy_store`
SET `lifecycle_status` = CASE
                             WHEN `status` = 1 THEN 30
                             ELSE 40
    END
WHERE `lifecycle_status` IS NULL;

-- 门店分类层级字段
SET @col_category_parent_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'hxy_store_category'
      AND column_name = 'parent_id'
);
SET @sql_add_category_parent := IF(@col_category_parent_exists = 0,
                                   'ALTER TABLE `hxy_store_category` ADD COLUMN `parent_id` bigint NOT NULL DEFAULT 0 COMMENT ''父分类编号，0 表示一级分类'' AFTER `name`',
                                   'SELECT 1');
PREPARE stmt_add_category_parent FROM @sql_add_category_parent;
EXECUTE stmt_add_category_parent;
DEALLOCATE PREPARE stmt_add_category_parent;

SET @col_category_level_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'hxy_store_category'
      AND column_name = 'level'
);
SET @sql_add_category_level := IF(@col_category_level_exists = 0,
                                  'ALTER TABLE `hxy_store_category` ADD COLUMN `level` tinyint NOT NULL DEFAULT 1 COMMENT ''层级：1 一级分类 2 二级分类'' AFTER `parent_id`',
                                  'SELECT 1');
PREPARE stmt_add_category_level FROM @sql_add_category_level;
EXECUTE stmt_add_category_level;
DEALLOCATE PREPARE stmt_add_category_level;

UPDATE `hxy_store_category`
SET `parent_id` = IFNULL(`parent_id`, 0),
    `level`     = CASE WHEN IFNULL(`parent_id`, 0) > 0 THEN 2 ELSE 1 END;

-- 门店标签组主表
CREATE TABLE IF NOT EXISTS `hxy_store_tag_group`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`             varchar(64)  NOT NULL COMMENT '标签组编码',
    `name`             varchar(128) NOT NULL COMMENT '标签组名称',
    `required`         tinyint      NOT NULL DEFAULT 0 COMMENT '是否必选：0 否 1 是',
    `mutex`            tinyint      NOT NULL DEFAULT 0 COMMENT '是否互斥：0 否 1 是',
    `editable_by_store` tinyint     NOT NULL DEFAULT 0 COMMENT '门店是否可编辑：0 否 1 是',
    `status`           tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0 停用 1 启用',
    `sort`             int          NOT NULL DEFAULT 0 COMMENT '排序',
    `remark`           varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
    `creator`          varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`          varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`        bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`),
    UNIQUE KEY `uk_tenant_name` (`tenant_id`, `name`),
    KEY `idx_status` (`status`)
) COMMENT ='门店标签组表';

-- 门店标签增加 group_id
SET @col_tag_group_id_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'hxy_store_tag'
      AND column_name = 'group_id'
);
SET @sql_add_tag_group_id := IF(@col_tag_group_id_exists = 0,
                                'ALTER TABLE `hxy_store_tag` ADD COLUMN `group_id` bigint DEFAULT NULL COMMENT ''标签组编号'' AFTER `name`',
                                'SELECT 1');
PREPARE stmt_add_tag_group_id FROM @sql_add_tag_group_id;
EXECUTE stmt_add_tag_group_id;
DEALLOCATE PREPARE stmt_add_tag_group_id;

-- 仅在索引缺失时新增索引
SET @idx_group_id_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'hxy_store_tag'
      AND index_name = 'idx_group_id'
);
SET @sql_add_idx_group_id := IF(@idx_group_id_exists = 0,
                                'ALTER TABLE `hxy_store_tag` ADD INDEX `idx_group_id` (`group_id`)',
                                'SELECT 1');
PREPARE stmt_add_idx_group_id FROM @sql_add_idx_group_id;
EXECUTE stmt_add_idx_group_id;
DEALLOCATE PREPARE stmt_add_idx_group_id;

-- 预置标准标签组（多租户按 tenant_id 生效）
INSERT INTO `hxy_store_tag_group` (`code`, `name`, `required`, `mutex`, `editable_by_store`, `status`, `sort`, `remark`, `tenant_id`)
SELECT 'BIZ_ATTR', '经营属性', 1, 1, 0, 1, 100, '用于直营/联营等经营属性治理', tenant_id
FROM (SELECT DISTINCT tenant_id FROM `hxy_store_tag`) t
WHERE NOT EXISTS (
    SELECT 1 FROM `hxy_store_tag_group` g WHERE g.tenant_id = t.tenant_id AND g.code = 'BIZ_ATTR'
);

INSERT INTO `hxy_store_tag_group` (`code`, `name`, `required`, `mutex`, `editable_by_store`, `status`, `sort`, `remark`, `tenant_id`)
SELECT 'SERVICE_CAP', '服务能力', 0, 0, 1, 1, 90, '用于门店服务能力标记', tenant_id
FROM (SELECT DISTINCT tenant_id FROM `hxy_store_tag`) t
WHERE NOT EXISTS (
    SELECT 1 FROM `hxy_store_tag_group` g WHERE g.tenant_id = t.tenant_id AND g.code = 'SERVICE_CAP'
);

INSERT INTO `hxy_store_tag_group` (`code`, `name`, `required`, `mutex`, `editable_by_store`, `status`, `sort`, `remark`, `tenant_id`)
SELECT 'CUSTOMER_SEG', '客群定位', 0, 0, 1, 1, 80, '用于门店目标客群标记', tenant_id
FROM (SELECT DISTINCT tenant_id FROM `hxy_store_tag`) t
WHERE NOT EXISTS (
    SELECT 1 FROM `hxy_store_tag_group` g WHERE g.tenant_id = t.tenant_id AND g.code = 'CUSTOMER_SEG'
);

INSERT INTO `hxy_store_tag_group` (`code`, `name`, `required`, `mutex`, `editable_by_store`, `status`, `sort`, `remark`, `tenant_id`)
SELECT 'FULFILLMENT', '履约类型', 1, 1, 0, 1, 70, '用于到店/到家等履约类型控制', tenant_id
FROM (SELECT DISTINCT tenant_id FROM `hxy_store_tag`) t
WHERE NOT EXISTS (
    SELECT 1 FROM `hxy_store_tag_group` g WHERE g.tenant_id = t.tenant_id AND g.code = 'FULFILLMENT'
);

-- 历史 group_name 自动补齐成标签组（未命中标准组时）
INSERT INTO `hxy_store_tag_group` (`code`, `name`, `required`, `mutex`, `editable_by_store`, `status`, `sort`, `remark`, `tenant_id`)
SELECT CONCAT('AUTO_', UPPER(SUBSTRING(MD5(tag_source.group_name), 1, 8))),
       tag_source.group_name,
       0,
       0,
       1,
       1,
       10,
       '由历史标签自动生成',
       tag_source.tenant_id
FROM (
         SELECT DISTINCT tenant_id, TRIM(group_name) AS group_name
         FROM `hxy_store_tag`
         WHERE group_name IS NOT NULL
           AND TRIM(group_name) <> ''
     ) tag_source
WHERE NOT EXISTS (
    SELECT 1
    FROM `hxy_store_tag_group` g
    WHERE g.tenant_id = tag_source.tenant_id
      AND g.name = tag_source.group_name
);

-- 回填标签 group_id
UPDATE `hxy_store_tag` t
    JOIN `hxy_store_tag_group` g
    ON g.tenant_id = t.tenant_id
        AND g.name = t.group_name
SET t.group_id = g.id
WHERE t.group_id IS NULL;

-- 若仍存在异常空值，兜底挂到“客群定位”
UPDATE `hxy_store_tag` t
    JOIN `hxy_store_tag_group` g
    ON g.tenant_id = t.tenant_id
        AND g.code = 'CUSTOMER_SEG'
SET t.group_id = g.id,
    t.group_name = g.name
WHERE t.group_id IS NULL;

-- 保持 group_name 与 group_id 对齐，兼容旧页面
UPDATE `hxy_store_tag` t
    JOIN `hxy_store_tag_group` g
    ON g.id = t.group_id
        AND g.tenant_id = t.tenant_id
SET t.group_name = g.name
WHERE t.group_name IS NULL
   OR t.group_name <> g.name;

SET FOREIGN_KEY_CHECKS = 1;
