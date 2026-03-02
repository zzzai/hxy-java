SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- HXY: 类目与属性模板（并轨版，保留现有 product_spu/product_sku 主链路）
-- 目标：
-- 1) 类目模板版本化（避免改模板影响历史订单/历史商品）
-- 2) SKU 规格组合自动生成（spec_hash 幂等）
-- 3) 为后续服务/零售多形态扩展预留 EAV 能力

CREATE TABLE IF NOT EXISTS `hxy_category_ext`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category_id`     bigint       NOT NULL COMMENT '关联 product_category.id',
    `code`            varchar(64)  NOT NULL COMMENT '类目编码',
    `level`           tinyint      NOT NULL DEFAULT '1' COMMENT '类目层级',
    `path`            varchar(255) NOT NULL DEFAULT '/' COMMENT '物化路径',
    `is_leaf`         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否叶子类目',
    `product_type`    tinyint      NOT NULL DEFAULT '1' COMMENT '商品类型：1实物 2服务 3卡项 4虚拟',
    `config_json`     text         NULL COMMENT '类目业务配置 JSON',
    `status`          tinyint      NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
    `creator`         varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`         varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`       bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_category_id` (`tenant_id`, `category_id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`),
    KEY `idx_tenant_level_status` (`tenant_id`, `level`, `status`)
) COMMENT ='类目扩展表（并轨）';

CREATE TABLE IF NOT EXISTS `hxy_attribute_definition`
(
    `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`              varchar(64)  NOT NULL COMMENT '属性编码',
    `name`              varchar(64)  NOT NULL COMMENT '属性名称',
    `data_type`         tinyint      NOT NULL COMMENT '数据类型：1字符串 2数字 3布尔 4单选 5多选 6日期 7日期时间 8JSON',
    `input_type`        tinyint      NOT NULL COMMENT '输入类型：1输入框 2文本框 3下拉 4多选下拉 5单选 6复选 7日期 8数字',
    `unit`              varchar(32)  NOT NULL DEFAULT '' COMMENT '单位',
    `is_system`         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否系统属性',
    `validation_json`   text         NULL COMMENT '校验规则 JSON',
    `status`            tinyint      NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
    `legacy_property_id` bigint      NULL COMMENT '可选：映射 product_property.id',
    `creator`           varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`           varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`         bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_tenant_legacy_property_id` (`tenant_id`, `legacy_property_id`)
) COMMENT ='属性定义表';

CREATE TABLE IF NOT EXISTS `hxy_attribute_option`
(
    `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `attribute_id`  bigint        NOT NULL COMMENT '属性定义ID',
    `value`         varchar(128)  NOT NULL COMMENT '选项值',
    `label`         varchar(128)  NOT NULL COMMENT '展示文案',
    `sort`          int           NOT NULL DEFAULT '0' COMMENT '排序',
    `extra_json`    text          NULL COMMENT '扩展信息 JSON',
    `status`        tinyint       NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
    `creator`       varchar(64)            DEFAULT '' COMMENT '创建者',
    `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`       varchar(64)            DEFAULT '' COMMENT '更新者',
    `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`     bigint        NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_attr_value` (`tenant_id`, `attribute_id`, `value`),
    KEY `idx_tenant_attr_status` (`tenant_id`, `attribute_id`, `status`)
) COMMENT ='属性选项表';

CREATE TABLE IF NOT EXISTS `hxy_attribute_group`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`          varchar(64)  NOT NULL COMMENT '分组编码',
    `name`          varchar(64)  NOT NULL COMMENT '分组名称',
    `sort`          int          NOT NULL DEFAULT '0' COMMENT '排序',
    `status`        tinyint      NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
    `creator`       varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`       varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`     bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`)
) COMMENT ='属性分组表';

CREATE TABLE IF NOT EXISTS `hxy_category_attr_tpl_version`
(
    `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category_id`       bigint       NOT NULL COMMENT '类目ID',
    `version_no`        int          NOT NULL COMMENT '模板版本号，类目内自增',
    `status`            tinyint      NOT NULL DEFAULT '0' COMMENT '状态：0草稿 1已发布 2归档',
    `remark`            varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
    `snapshot_json`     longtext     NULL COMMENT '模板快照 JSON',
    `published_by`      varchar(64)           DEFAULT '' COMMENT '发布人',
    `published_time`    datetime     NULL COMMENT '发布时间',
    `creator`           varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`           varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`         bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_category_version` (`tenant_id`, `category_id`, `version_no`),
    KEY `idx_tenant_category_status` (`tenant_id`, `category_id`, `status`)
) COMMENT ='类目属性模板版本表';

CREATE TABLE IF NOT EXISTS `hxy_category_attr_tpl_item`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `template_version_id`    bigint       NOT NULL COMMENT '模板版本ID',
    `attribute_id`           bigint       NOT NULL COMMENT '属性ID',
    `group_id`               bigint       NULL COMMENT '属性分组ID',
    `attr_role`              tinyint      NOT NULL COMMENT '属性角色：1SPU_ATTR 2SKU_SPEC 3SKU_ATTR 4SALE_ATTR',
    `is_required`            bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否必填',
    `is_searchable`          bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否可搜索',
    `is_filterable`          bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否可筛选',
    `is_comparable`          bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否可对比',
    `is_visible`             bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否展示',
    `affects_price`          bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否影响价格',
    `affects_stock`          bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否影响库存',
    `default_value`          varchar(512) NOT NULL DEFAULT '' COMMENT '默认值',
    `sort`                   int          NOT NULL DEFAULT '0' COMMENT '排序',
    `creator`                varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`            datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`                varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`            datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`                bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`              bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_tpl_attr` (`tenant_id`, `template_version_id`, `attribute_id`),
    KEY `idx_tenant_tpl_role` (`tenant_id`, `template_version_id`, `attr_role`)
) COMMENT ='类目属性模板项';

CREATE TABLE IF NOT EXISTS `hxy_spu_attr_value`
(
    `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `spu_id`         bigint        NOT NULL COMMENT 'SPU ID',
    `attribute_id`   bigint        NOT NULL COMMENT '属性ID',
    `string_value`   varchar(1024) NOT NULL DEFAULT '' COMMENT '字符串值',
    `number_value`   decimal(20,4) NULL COMMENT '数值',
    `bool_value`     bit(1)        NULL COMMENT '布尔值',
    `json_value`     text          NULL COMMENT 'JSON 值',
    `option_ids`     varchar(512)  NOT NULL DEFAULT '' COMMENT '选项ID列表，逗号分隔',
    `creator`        varchar(64)            DEFAULT '' COMMENT '创建者',
    `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`        varchar(64)            DEFAULT '' COMMENT '更新者',
    `update_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`      bigint        NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_spu_attr` (`tenant_id`, `spu_id`, `attribute_id`),
    KEY `idx_tenant_attr` (`tenant_id`, `attribute_id`)
) COMMENT ='SPU 属性值表';

CREATE TABLE IF NOT EXISTS `hxy_sku_attr_value`
(
    `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sku_id`         bigint        NOT NULL COMMENT 'SKU ID',
    `attribute_id`   bigint        NOT NULL COMMENT '属性ID',
    `string_value`   varchar(1024) NOT NULL DEFAULT '' COMMENT '字符串值',
    `number_value`   decimal(20,4) NULL COMMENT '数值',
    `bool_value`     bit(1)        NULL COMMENT '布尔值',
    `json_value`     text          NULL COMMENT 'JSON 值',
    `option_ids`     varchar(512)  NOT NULL DEFAULT '' COMMENT '选项ID列表，逗号分隔',
    `creator`        varchar(64)            DEFAULT '' COMMENT '创建者',
    `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`        varchar(64)            DEFAULT '' COMMENT '更新者',
    `update_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`      bigint        NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_sku_attr` (`tenant_id`, `sku_id`, `attribute_id`),
    KEY `idx_tenant_attr` (`tenant_id`, `attribute_id`)
) COMMENT ='SKU 属性值表';

CREATE TABLE IF NOT EXISTS `hxy_sku_spec_combo`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `spu_id`           bigint       NOT NULL COMMENT 'SPU ID',
    `spec_hash`        varchar(64)  NOT NULL COMMENT '规格哈希（排序后 optionIds 生成）',
    `spec_json`        text         NOT NULL COMMENT '规格详情 JSON',
    `sku_name_suffix`  varchar(255) NOT NULL DEFAULT '' COMMENT '规格文案摘要',
    `status`           tinyint      NOT NULL DEFAULT '0' COMMENT '状态：0待生成 1已生成 2失败',
    `target_sku_id`    bigint       NULL COMMENT '生成后的 SKU ID',
    `error_msg`        varchar(512) NOT NULL DEFAULT '' COMMENT '失败原因',
    `creator`          varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`          varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`        bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_spu_spec_hash` (`tenant_id`, `spu_id`, `spec_hash`),
    KEY `idx_tenant_spu_status` (`tenant_id`, `spu_id`, `status`)
) COMMENT ='SPU 规格组合候选表';

CREATE TABLE IF NOT EXISTS `hxy_sku_generate_task`
(
    `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_no`               varchar(64)  NOT NULL COMMENT '任务编号',
    `spu_id`                bigint       NOT NULL COMMENT 'SPU ID',
    `category_id`           bigint       NOT NULL COMMENT '类目ID',
    `template_version_id`   bigint       NOT NULL COMMENT '模板版本ID',
    `mode`                  tinyint      NOT NULL COMMENT '模式：1预览 2提交',
    `idempotency_key`       varchar(128) NOT NULL DEFAULT '' COMMENT '幂等键（提交模式使用）',
    `status`                tinyint      NOT NULL DEFAULT '0' COMMENT '状态：0待处理 1处理中 2成功 3部分成功 4失败',
    `request_json`          longtext     NULL COMMENT '请求快照 JSON',
    `result_json`           longtext     NULL COMMENT '结果快照 JSON',
    `error_msg`             varchar(512) NOT NULL DEFAULT '' COMMENT '错误信息',
    `retry_count`           int          NOT NULL DEFAULT '0' COMMENT '重试次数',
    `next_retry_time`       datetime     NULL COMMENT '下次重试时间',
    `operator_id`           bigint       NULL COMMENT '操作人',
    `creator`               varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`               varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`             bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_task_no` (`tenant_id`, `task_no`),
    UNIQUE KEY `uk_tenant_spu_idempotency` (`tenant_id`, `spu_id`, `mode`, `idempotency_key`),
    KEY `idx_tenant_status_retry` (`tenant_id`, `status`, `next_retry_time`)
) COMMENT ='SKU 自动生成任务表';

CREATE TABLE IF NOT EXISTS `hxy_sku_generate_task_item`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id`        bigint       NOT NULL COMMENT '任务ID',
    `spu_id`         bigint       NOT NULL COMMENT 'SPU ID',
    `spec_hash`      varchar(64)  NOT NULL COMMENT '规格哈希',
    `spec_json`      text         NOT NULL COMMENT '规格明细',
    `target_sku_id`  bigint       NULL COMMENT '生成的 SKU ID',
    `status`         tinyint      NOT NULL DEFAULT '0' COMMENT '状态：0待处理 1成功 2失败 3跳过',
    `error_msg`      varchar(512) NOT NULL DEFAULT '' COMMENT '错误信息',
    `creator`        varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`        varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`      bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_task_spec_hash` (`tenant_id`, `task_id`, `spec_hash`),
    KEY `idx_tenant_task_status` (`tenant_id`, `task_id`, `status`)
) COMMENT ='SKU 自动生成任务明细表';

SET FOREIGN_KEY_CHECKS = 1;
