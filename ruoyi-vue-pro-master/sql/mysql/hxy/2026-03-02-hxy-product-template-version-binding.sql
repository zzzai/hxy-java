-- HXY: SPU/SKU 主数据显式绑定 templateVersionId（服务商品优先）

ALTER TABLE `product_spu`
    ADD COLUMN IF NOT EXISTS `template_version_id` bigint NULL COMMENT '类目模板版本ID' AFTER `product_type`;

ALTER TABLE `product_sku`
    ADD COLUMN IF NOT EXISTS `template_version_id` bigint NULL COMMENT '类目模板版本ID' AFTER `spu_id`;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'product_spu'
      AND index_name = 'idx_product_spu_template_version_id'
);
SET @idx_sql := IF(@idx_exists = 0,
                   'ALTER TABLE `product_spu` ADD INDEX `idx_product_spu_template_version_id` (`template_version_id`)',
                   'SELECT 1');
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'product_sku'
      AND index_name = 'idx_product_sku_template_version_id'
);
SET @idx_sql := IF(@idx_exists = 0,
                   'ALTER TABLE `product_sku` ADD INDEX `idx_product_sku_template_version_id` (`template_version_id`)',
                   'SELECT 1');
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1) 回填服务商品 SPU 的模板版本：按类目取最新已发布模板版本
UPDATE `product_spu` spu
         JOIN (
    SELECT `category_id`, MAX(`id`) AS `template_version_id`
    FROM `hxy_category_attr_tpl_version`
    WHERE `status` = 1
    GROUP BY `category_id`
) tpl ON tpl.`category_id` = spu.`category_id`
SET spu.`template_version_id` = tpl.`template_version_id`
WHERE spu.`deleted` = b'0'
  AND spu.`product_type` = 2
  AND spu.`template_version_id` IS NULL;

-- 2) 回填 SKU 模板版本：继承所属 SPU
UPDATE `product_sku` sku
         JOIN `product_spu` spu ON spu.`id` = sku.`spu_id` AND spu.`deleted` = b'0'
SET sku.`template_version_id` = spu.`template_version_id`
WHERE sku.`deleted` = b'0'
  AND sku.`template_version_id` IS NULL
  AND spu.`template_version_id` IS NOT NULL;
