SET NAMES utf8mb4;

-- HXY: 门店生命周期守卫-库存流水分状态模式配置（兼容旧 stock-flow.mode）

INSERT INTO infra_config (category, type, name, config_key, value, visible, remark, creator, updater, deleted)
SELECT 'mall.store.lifecycle.guard', 2, seed.name, seed.config_key,
       COALESCE(
               (SELECT c.value
                FROM infra_config c
                WHERE c.config_key = 'hxy.store.lifecycle.guard.stock-flow.mode'
                  AND c.deleted = 0
                ORDER BY c.id DESC
                LIMIT 1),
               'BLOCK'
       ),
       b'0', '门店生命周期守卫库存流水分状态模式：WARN 仅审计不阻塞，BLOCK 阻塞流转', '1', '1', b'0'
FROM (
         SELECT '门店守卫-库存流水待处理模式' AS name, 'hxy.store.lifecycle.guard.stock-flow.pending.mode' AS config_key
         UNION ALL SELECT '门店守卫-库存流水处理中模式', 'hxy.store.lifecycle.guard.stock-flow.processing.mode'
         UNION ALL SELECT '门店守卫-库存流水失败模式', 'hxy.store.lifecycle.guard.stock-flow.failed.mode'
     ) seed
WHERE NOT EXISTS (
    SELECT 1 FROM infra_config c
    WHERE c.config_key = seed.config_key
      AND c.deleted = 0
);
