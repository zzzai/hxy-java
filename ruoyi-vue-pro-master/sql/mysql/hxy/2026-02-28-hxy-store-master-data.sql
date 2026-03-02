SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- HXY: 门店主数据（门店/分类/标签）

CREATE TABLE IF NOT EXISTS `hxy_store_category`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`        varchar(64)  NOT NULL COMMENT '分类编码',
    `name`        varchar(128) NOT NULL COMMENT '分类名称',
    `status`      tinyint      NOT NULL DEFAULT '1' COMMENT '状态：0 停用 1 启用',
    `sort`        int          NOT NULL DEFAULT '0' COMMENT '排序',
    `remark`      varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
    `creator`     varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`   bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`),
    UNIQUE KEY `uk_tenant_name` (`tenant_id`, `name`),
    KEY `idx_status` (`status`)
) COMMENT ='门店分类表';

CREATE TABLE IF NOT EXISTS `hxy_store_tag`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`        varchar(64)  NOT NULL COMMENT '标签编码',
    `name`        varchar(128) NOT NULL COMMENT '标签名称',
    `group_name`  varchar(128) NOT NULL DEFAULT '默认标签组' COMMENT '标签组',
    `status`      tinyint      NOT NULL DEFAULT '1' COMMENT '状态：0 停用 1 启用',
    `sort`        int          NOT NULL DEFAULT '0' COMMENT '排序',
    `remark`      varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
    `creator`     varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`   bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`),
    UNIQUE KEY `uk_tenant_name` (`tenant_id`, `name`),
    KEY `idx_status` (`status`),
    KEY `idx_group_name` (`group_name`)
) COMMENT ='门店标签表';

CREATE TABLE IF NOT EXISTS `hxy_store`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`          varchar(64)  NOT NULL COMMENT '门店编码',
    `name`          varchar(128) NOT NULL COMMENT '门店名称',
    `short_name`    varchar(128) NOT NULL DEFAULT '' COMMENT '门店简称',
    `category_id`   bigint       NOT NULL COMMENT '门店分类编号',
    `status`        tinyint      NOT NULL DEFAULT '1' COMMENT '状态：0 停用 1 启用',
    `contact_name`  varchar(64)  NOT NULL DEFAULT '' COMMENT '联系人',
    `contact_mobile` varchar(32) NOT NULL DEFAULT '' COMMENT '联系电话',
    `province_code` varchar(32)  NOT NULL DEFAULT '' COMMENT '省编码',
    `city_code`     varchar(32)  NOT NULL DEFAULT '' COMMENT '市编码',
    `district_code` varchar(32)  NOT NULL DEFAULT '' COMMENT '区编码',
    `address`       varchar(255) NOT NULL DEFAULT '' COMMENT '详细地址',
    `longitude`     decimal(10,6)         DEFAULT NULL COMMENT '经度',
    `latitude`      decimal(10,6)         DEFAULT NULL COMMENT '纬度',
    `opening_time`  varchar(16)  NOT NULL DEFAULT '' COMMENT '营业开始时间',
    `closing_time`  varchar(16)  NOT NULL DEFAULT '' COMMENT '营业结束时间',
    `sort`          int          NOT NULL DEFAULT '0' COMMENT '排序',
    `remark`        varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
    `creator`       varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`       varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`     bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_contact_mobile` (`contact_mobile`)
) COMMENT ='门店主数据表';

CREATE TABLE IF NOT EXISTS `hxy_store_tag_rel`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `store_id`    bigint   NOT NULL COMMENT '门店编号',
    `tag_id`      bigint   NOT NULL COMMENT '标签编号',
    `creator`     varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)   NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`   bigint   NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_store_tag` (`tenant_id`, `store_id`, `tag_id`),
    KEY `idx_store_id` (`store_id`),
    KEY `idx_tag_id` (`tag_id`)
) COMMENT ='门店标签关联表';

SET FOREIGN_KEY_CHECKS = 1;
