-- 支付日对账检查 SQL
-- 使用前建议设置：
--   SET @window_start = '2026-02-21 00:00:00';
--   SET @window_end = '2026-02-22 00:00:00';
--   SET @stale_minutes = 10;
--
-- 输出列：
--   severity | issue_type | entity_type | biz_key | code | detail | expected_amount | actual_amount | occurred_at
--
-- 说明：
--   - severity=BLOCK：必须拦截处理
--   - severity=WARN：可继续观察，但建议处理

SELECT severity,
       issue_type,
       entity_type,
       biz_key,
       code,
       detail,
       expected_amount,
       actual_amount,
       occurred_at
FROM (
         -- ==================== 订单对账 ====================
         SELECT 'BLOCK'                              AS severity,
                'STATUS_DIFF'                        AS issue_type,
                'ORDER'                              AS entity_type,
                o.merchant_order_id                  AS biz_key,
                'O01_ORDER_SUCCESS_NO_CHANNEL_NO'    AS code,
                CONCAT('order_id=', o.id, ', status=', o.status) AS detail,
                CAST(o.price AS CHAR)                AS expected_amount,
                NULL                                 AS actual_amount,
                DATE_FORMAT(o.update_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_order o
         WHERE o.deleted = b'0'
           AND o.create_time >= @window_start
           AND o.create_time < @window_end
           AND o.status IN (10, 20)
           AND (o.channel_order_no IS NULL OR LENGTH(TRIM(o.channel_order_no)) = 0)

         UNION ALL

         SELECT 'BLOCK'                              AS severity,
                'STATUS_DIFF'                        AS issue_type,
                'ORDER'                              AS entity_type,
                o.merchant_order_id                  AS biz_key,
                'O02_ORDER_CHANNEL_NO_STATUS_MISMATCH' AS code,
                CONCAT('order_id=', o.id, ', status=', o.status, ', channel_order_no=', o.channel_order_no) AS detail,
                CAST(o.price AS CHAR)                AS expected_amount,
                NULL                                 AS actual_amount,
                DATE_FORMAT(o.update_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_order o
         WHERE o.deleted = b'0'
           AND o.create_time >= @window_start
           AND o.create_time < @window_end
           AND (o.channel_order_no IS NOT NULL AND LENGTH(TRIM(o.channel_order_no)) > 0)
           AND o.status NOT IN (10, 20)

         UNION ALL

         SELECT 'BLOCK'                              AS severity,
                'STATUS_DIFF'                        AS issue_type,
                'ORDER'                              AS entity_type,
                o.merchant_order_id                  AS biz_key,
                'O03_ORDER_SUCCESS_NO_SUCCESS_EXTENSION' AS code,
                CONCAT('order_id=', o.id, ', success_extension_count=',
                       SUM(CASE WHEN e.status = 10 THEN 1 ELSE 0 END)) AS detail,
                CAST(o.price AS CHAR)                AS expected_amount,
                NULL                                 AS actual_amount,
                DATE_FORMAT(o.update_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_order o
                  LEFT JOIN pay_order_extension e
                            ON e.order_id = o.id
                                AND e.deleted = b'0'
         WHERE o.deleted = b'0'
           AND o.create_time >= @window_start
           AND o.create_time < @window_end
         GROUP BY o.id, o.merchant_order_id, o.price, o.status, o.update_time
         HAVING o.status = 10
            AND SUM(CASE WHEN e.status = 10 THEN 1 ELSE 0 END) = 0

         UNION ALL

         SELECT 'WARN'                               AS severity,
                'STATUS_DIFF'                        AS issue_type,
                'ORDER'                              AS entity_type,
                o.merchant_order_id                  AS biz_key,
                'O04_ORDER_MULTI_SUCCESS_EXTENSION'  AS code,
                CONCAT('order_id=', o.id, ', success_extension_count=',
                       SUM(CASE WHEN e.status = 10 THEN 1 ELSE 0 END)) AS detail,
                CAST(o.price AS CHAR)                AS expected_amount,
                NULL                                 AS actual_amount,
                DATE_FORMAT(o.update_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_order o
                  JOIN pay_order_extension e
                       ON e.order_id = o.id
                           AND e.deleted = b'0'
         WHERE o.deleted = b'0'
           AND o.create_time >= @window_start
           AND o.create_time < @window_end
         GROUP BY o.id, o.merchant_order_id, o.price, o.update_time
         HAVING SUM(CASE WHEN e.status = 10 THEN 1 ELSE 0 END) > 1

         UNION ALL

         SELECT 'WARN'                               AS severity,
                'STATUS_DIFF'                        AS issue_type,
                'ORDER'                              AS entity_type,
                o.merchant_order_id                  AS biz_key,
                'O05_ORDER_WAITING_EXPIRED'          AS code,
                CONCAT('order_id=', o.id, ', expire_time=', DATE_FORMAT(o.expire_time, '%Y-%m-%d %H:%i:%s')) AS detail,
                CAST(o.price AS CHAR)                AS expected_amount,
                NULL                                 AS actual_amount,
                DATE_FORMAT(o.expire_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_order o
         WHERE o.deleted = b'0'
           AND o.create_time >= @window_start
           AND o.create_time < @window_end
           AND o.status = 0
           AND o.expire_time < NOW()

         UNION ALL

         SELECT 'BLOCK'                              AS severity,
                'AMOUNT_DIFF'                        AS issue_type,
                'ORDER'                              AS entity_type,
                o.merchant_order_id                  AS biz_key,
                'O06_ORDER_REFUND_PRICE_MISMATCH'    AS code,
                CONCAT('order_id=', o.id, ', order_refund_price=', o.refund_price,
                       ', success_refund_sum=', IFNULL(SUM(CASE WHEN r.status = 10 THEN r.refund_price ELSE 0 END), 0)) AS detail,
                CAST(o.refund_price AS CHAR)         AS expected_amount,
                CAST(IFNULL(SUM(CASE WHEN r.status = 10 THEN r.refund_price ELSE 0 END), 0) AS CHAR) AS actual_amount,
                DATE_FORMAT(o.update_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_order o
                  LEFT JOIN pay_refund r
                            ON r.order_id = o.id
                                AND r.deleted = b'0'
         WHERE o.deleted = b'0'
           AND o.create_time >= @window_start
           AND o.create_time < @window_end
           AND o.status = 20
         GROUP BY o.id, o.merchant_order_id, o.refund_price, o.update_time
         HAVING IFNULL(SUM(CASE WHEN r.status = 10 THEN r.refund_price ELSE 0 END), 0) <> IFNULL(o.refund_price, 0)

         UNION ALL

         SELECT 'BLOCK'                              AS severity,
                'ORPHAN_FLOW'                        AS issue_type,
                'ORDER_NOTIFY_TASK'                  AS entity_type,
                COALESCE(t.merchant_order_id, CONCAT('task#', t.id)) AS biz_key,
                'O07_ORDER_NOTIFY_TASK_ORPHAN'       AS code,
                CONCAT('task_id=', t.id, ', data_id=', t.data_id) AS detail,
                NULL                                 AS expected_amount,
                NULL                                 AS actual_amount,
                DATE_FORMAT(t.create_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_notify_task t
                  LEFT JOIN pay_order o
                            ON o.id = t.data_id
                                AND o.deleted = b'0'
         WHERE t.deleted = b'0'
           AND t.type = 1
           AND t.create_time >= @window_start
           AND t.create_time < @window_end
           AND o.id IS NULL

         UNION ALL

         SELECT 'WARN'                               AS severity,
                'STATUS_DIFF'                        AS issue_type,
                'ORDER_NOTIFY_TASK'                  AS entity_type,
                COALESCE(t.merchant_order_id, CONCAT('task#', t.id)) AS biz_key,
                'O08_ORDER_NOTIFY_TASK_STUCK'        AS code,
                CONCAT('task_id=', t.id, ', status=', t.status, ', notify_times=', t.notify_times,
                       '/', t.max_notify_times) AS detail,
                NULL                                 AS expected_amount,
                NULL                                 AS actual_amount,
                DATE_FORMAT(COALESCE(t.last_execute_time, t.update_time, t.create_time), '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_notify_task t
                  JOIN pay_order o
                       ON o.id = t.data_id
                           AND o.deleted = b'0'
         WHERE t.deleted = b'0'
           AND t.type = 1
           AND t.status IN (0, 20, 21, 22)
           AND t.update_time >= @window_start
           AND t.update_time < @window_end
           AND COALESCE(t.last_execute_time, t.update_time, t.create_time) <
               DATE_SUB(NOW(), INTERVAL IFNULL(@stale_minutes, 10) MINUTE)

         -- ==================== 退款对账 ====================
         UNION ALL

         SELECT 'BLOCK'                              AS severity,
                'ORPHAN_FLOW'                        AS issue_type,
                'REFUND'                             AS entity_type,
                r.merchant_refund_id                 AS biz_key,
                'R01_REFUND_ORPHAN_ORDER'            AS code,
                CONCAT('refund_id=', r.id, ', order_id=', r.order_id) AS detail,
                CAST(r.pay_price AS CHAR)            AS expected_amount,
                NULL                                 AS actual_amount,
                DATE_FORMAT(r.create_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_refund r
                  LEFT JOIN pay_order o
                            ON o.id = r.order_id
                                AND o.deleted = b'0'
         WHERE r.deleted = b'0'
           AND r.create_time >= @window_start
           AND r.create_time < @window_end
           AND o.id IS NULL

         UNION ALL

         SELECT 'BLOCK'                              AS severity,
                'AMOUNT_DIFF'                        AS issue_type,
                'REFUND'                             AS entity_type,
                r.merchant_refund_id                 AS biz_key,
                'R02_REFUND_PAY_PRICE_MISMATCH_ORDER' AS code,
                CONCAT('refund_id=', r.id, ', order_id=', r.order_id) AS detail,
                CAST(o.price AS CHAR)                AS expected_amount,
                CAST(r.pay_price AS CHAR)            AS actual_amount,
                DATE_FORMAT(r.update_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_refund r
                  JOIN pay_order o
                       ON o.id = r.order_id
                           AND o.deleted = b'0'
         WHERE r.deleted = b'0'
           AND r.create_time >= @window_start
           AND r.create_time < @window_end
           AND r.pay_price <> o.price

         UNION ALL

         SELECT 'BLOCK'                              AS severity,
                'AMOUNT_DIFF'                        AS issue_type,
                'REFUND'                             AS entity_type,
                r.merchant_refund_id                 AS biz_key,
                'R03_REFUND_AMOUNT_GT_ORDER'         AS code,
                CONCAT('refund_id=', r.id, ', order_id=', r.order_id) AS detail,
                CAST(o.price AS CHAR)                AS expected_amount,
                CAST(r.refund_price AS CHAR)         AS actual_amount,
                DATE_FORMAT(r.update_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_refund r
                  JOIN pay_order o
                       ON o.id = r.order_id
                           AND o.deleted = b'0'
         WHERE r.deleted = b'0'
           AND r.create_time >= @window_start
           AND r.create_time < @window_end
           AND r.refund_price > o.price

         UNION ALL

         SELECT 'BLOCK'                              AS severity,
                'STATUS_DIFF'                        AS issue_type,
                'REFUND'                             AS entity_type,
                r.merchant_refund_id                 AS biz_key,
                'R04_REFUND_SUCCESS_NO_CHANNEL_REFUND_NO' AS code,
                CONCAT('refund_id=', r.id, ', status=', r.status) AS detail,
                CAST(r.refund_price AS CHAR)         AS expected_amount,
                NULL                                 AS actual_amount,
                DATE_FORMAT(r.update_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_refund r
         WHERE r.deleted = b'0'
           AND r.create_time >= @window_start
           AND r.create_time < @window_end
           AND r.status = 10
           AND (r.channel_refund_no IS NULL OR LENGTH(TRIM(r.channel_refund_no)) = 0)

         UNION ALL

         SELECT 'BLOCK'                              AS severity,
                'STATUS_DIFF'                        AS issue_type,
                'REFUND'                             AS entity_type,
                r.merchant_refund_id                 AS biz_key,
                'R05_REFUND_CHANNEL_REFUND_NO_STATUS_MISMATCH' AS code,
                CONCAT('refund_id=', r.id, ', status=', r.status, ', channel_refund_no=', r.channel_refund_no) AS detail,
                CAST(r.refund_price AS CHAR)         AS expected_amount,
                NULL                                 AS actual_amount,
                DATE_FORMAT(r.update_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_refund r
         WHERE r.deleted = b'0'
           AND r.create_time >= @window_start
           AND r.create_time < @window_end
           AND (r.channel_refund_no IS NOT NULL AND LENGTH(TRIM(r.channel_refund_no)) > 0)
           AND r.status <> 10

         UNION ALL

         SELECT 'WARN'                               AS severity,
                'STATUS_DIFF'                        AS issue_type,
                'REFUND'                             AS entity_type,
                r.merchant_refund_id                 AS biz_key,
                'R06_REFUND_WAITING_TOO_LONG'        AS code,
                CONCAT('refund_id=', r.id, ', create_time=', DATE_FORMAT(r.create_time, '%Y-%m-%d %H:%i:%s')) AS detail,
                CAST(r.refund_price AS CHAR)         AS expected_amount,
                NULL                                 AS actual_amount,
                DATE_FORMAT(r.create_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_refund r
         WHERE r.deleted = b'0'
           AND r.create_time >= @window_start
           AND r.create_time < @window_end
           AND r.status = 0
           AND r.create_time < DATE_SUB(NOW(), INTERVAL IFNULL(@stale_minutes, 10) MINUTE)

         UNION ALL

         SELECT 'BLOCK'                              AS severity,
                'ORPHAN_FLOW'                        AS issue_type,
                'REFUND_NOTIFY_TASK'                 AS entity_type,
                COALESCE(t.merchant_order_id, CONCAT('task#', t.id)) AS biz_key,
                'R07_REFUND_NOTIFY_TASK_ORPHAN'      AS code,
                CONCAT('task_id=', t.id, ', data_id=', t.data_id) AS detail,
                NULL                                 AS expected_amount,
                NULL                                 AS actual_amount,
                DATE_FORMAT(t.create_time, '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_notify_task t
                  LEFT JOIN pay_refund r
                            ON r.id = t.data_id
                                AND r.deleted = b'0'
         WHERE t.deleted = b'0'
           AND t.type = 2
           AND t.create_time >= @window_start
           AND t.create_time < @window_end
           AND r.id IS NULL

         UNION ALL

         SELECT 'WARN'                               AS severity,
                'STATUS_DIFF'                        AS issue_type,
                'REFUND_NOTIFY_TASK'                 AS entity_type,
                COALESCE(t.merchant_order_id, CONCAT('task#', t.id)) AS biz_key,
                'R08_REFUND_NOTIFY_TASK_STUCK'       AS code,
                CONCAT('task_id=', t.id, ', status=', t.status, ', notify_times=', t.notify_times,
                       '/', t.max_notify_times) AS detail,
                NULL                                 AS expected_amount,
                NULL                                 AS actual_amount,
                DATE_FORMAT(COALESCE(t.last_execute_time, t.update_time, t.create_time), '%Y-%m-%d %H:%i:%s') AS occurred_at
         FROM pay_notify_task t
                  JOIN pay_refund r
                       ON r.id = t.data_id
                           AND r.deleted = b'0'
         WHERE t.deleted = b'0'
           AND t.type = 2
           AND t.status IN (0, 20, 21, 22)
           AND t.update_time >= @window_start
           AND t.update_time < @window_end
           AND COALESCE(t.last_execute_time, t.update_time, t.create_time) <
               DATE_SUB(NOW(), INTERVAL IFNULL(@stale_minutes, 10) MINUTE)
     ) t
ORDER BY CASE t.severity WHEN 'BLOCK' THEN 1 ELSE 2 END,
         t.issue_type,
         t.code,
         t.biz_key;
