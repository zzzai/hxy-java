-- HXY: product_spu schema compatibility fix for current product module code
-- Date: 2026-02-27

SET NAMES utf8mb4;

-- 1) Current code writes deliveryTypes -> delivery_types (comma-separated)
DROP PROCEDURE IF EXISTS hxy_add_delivery_types_if_absent;
DELIMITER $$
CREATE PROCEDURE hxy_add_delivery_types_if_absent()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'product_spu'
          AND column_name = 'delivery_types'
    ) THEN
        ALTER TABLE `product_spu`
            ADD COLUMN `delivery_types` varchar(64) DEFAULT '2' COMMENT '配送方式数组，逗号分隔';
    END IF;
END$$
DELIMITER ;

CALL hxy_add_delivery_types_if_absent();
DROP PROCEDURE IF EXISTS hxy_add_delivery_types_if_absent;

-- 2) Legacy table columns without defaults can break insert when field is absent in current DO
ALTER TABLE `product_spu`
    MODIFY COLUMN `bar_code` varchar(64) NOT NULL DEFAULT '' COMMENT '条形码',
    MODIFY COLUMN `unit` tinyint NOT NULL DEFAULT 0 COMMENT '单位',
    MODIFY COLUMN `delivery_template_id` bigint NOT NULL DEFAULT 0 COMMENT '物流配置模板编号',
    MODIFY COLUMN `recommend_hot` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否热卖推荐: 0 默认 1 热卖',
    MODIFY COLUMN `recommend_benefit` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否优惠推荐: 0 默认 1 优选',
    MODIFY COLUMN `recommend_best` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否精品推荐: 0 默认 1 精品',
    MODIFY COLUMN `recommend_new` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否新品推荐: 0 默认 1 新品',
    MODIFY COLUMN `recommend_good` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否优品推荐',
    MODIFY COLUMN `give_integral` int NOT NULL DEFAULT 0 COMMENT '赠送积分',
    MODIFY COLUMN `sub_commission_type` bit(1) NOT NULL DEFAULT b'0' COMMENT '分销类型';

-- 3) Normalize existing rows (single-tenant local environment)
UPDATE `product_spu` SET `tenant_id` = 1 WHERE `tenant_id` = 0;

-- 4) product_sku brokerage column compatibility
DROP PROCEDURE IF EXISTS hxy_add_sku_brokerage_cols_if_absent;
DELIMITER $$
CREATE PROCEDURE hxy_add_sku_brokerage_cols_if_absent()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'product_sku'
          AND column_name = 'first_brokerage_price'
    ) THEN
        ALTER TABLE `product_sku`
            ADD COLUMN `first_brokerage_price` INT DEFAULT NULL COMMENT '一级分销的佣金，单位：分';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'product_sku'
          AND column_name = 'second_brokerage_price'
    ) THEN
        ALTER TABLE `product_sku`
            ADD COLUMN `second_brokerage_price` INT DEFAULT NULL COMMENT '二级分销的佣金，单位：分';
    END IF;
END$$
DELIMITER ;

CALL hxy_add_sku_brokerage_cols_if_absent();
DROP PROCEDURE IF EXISTS hxy_add_sku_brokerage_cols_if_absent;

UPDATE `product_sku` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
