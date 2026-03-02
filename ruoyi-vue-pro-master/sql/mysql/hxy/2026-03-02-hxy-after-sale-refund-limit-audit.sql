SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 售后退款上限审计字段
SET @ddl := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'trade_after_sale'
          AND column_name = 'refund_limit_source'
    ),
    'SELECT 1',
    'ALTER TABLE `trade_after_sale` ADD COLUMN `refund_limit_source` varchar(64) NOT NULL DEFAULT '''' COMMENT ''退款上限来源'' AFTER `refund_price`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'trade_after_sale'
          AND column_name = 'refund_limit_detail_json'
    ),
    'SELECT 1',
    'ALTER TABLE `trade_after_sale` ADD COLUMN `refund_limit_detail_json` varchar(2000) NOT NULL DEFAULT '''' COMMENT ''退款上限审计明细快照(JSON)'' AFTER `refund_limit_source`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
