-- 支付回调回放验收 SQL（按商户单号）
-- 使用前建议设置：
--   SET @merchant_order_id = 'YOUR_MERCHANT_ORDER_ID';
--   SET @lookback_hours = 48;
--   SET @grace_minutes = 10;
--
-- 输出：
--   severity | code | detail
-- 说明：
--   - severity=BLOCK：必须先修复
--   - severity=WARN：可继续观察，但不建议直接放行

SELECT severity, code, detail
FROM (
         SELECT 'BLOCK' AS severity,
                'B01_ORDER_NOT_FOUND' AS code,
                CONCAT('merchant_order_id=', @merchant_order_id) AS detail
         FROM dual
         WHERE (SELECT COUNT(*)
                FROM pay_order
                WHERE merchant_order_id = @merchant_order_id
                  AND deleted = b'0') = 0

         UNION ALL

         SELECT 'BLOCK' AS severity,
                'B02_MULTI_PAY_ORDER' AS code,
                CONCAT('count=', t.cnt, ', merchant_order_id=', @merchant_order_id) AS detail
         FROM (SELECT COUNT(*) AS cnt
               FROM pay_order
               WHERE merchant_order_id = @merchant_order_id
                 AND deleted = b'0') t
         WHERE t.cnt > 1

         UNION ALL

         SELECT 'BLOCK' AS severity,
                'B03_ORDER_CLOSED_BUT_SUCCESS_EXTENSION' AS code,
                CONCAT('order_id=', o.id) AS detail
         FROM pay_order o
                  JOIN pay_order_extension e
                       ON e.order_id = o.id
                           AND e.deleted = b'0'
         WHERE o.merchant_order_id = @merchant_order_id
           AND o.deleted = b'0'
         GROUP BY o.id, o.status
         HAVING o.status = 30
            AND SUM(CASE WHEN e.status = 10 THEN 1 ELSE 0 END) > 0

         UNION ALL

         SELECT 'BLOCK' AS severity,
                'B04_MULTI_SUCCESS_EXTENSION' AS code,
                CONCAT('order_id=', o.id, ', success_extension_count=',
                       SUM(CASE WHEN e.status = 10 THEN 1 ELSE 0 END)) AS detail
         FROM pay_order o
                  JOIN pay_order_extension e
                       ON e.order_id = o.id
                           AND e.deleted = b'0'
         WHERE o.merchant_order_id = @merchant_order_id
           AND o.deleted = b'0'
         GROUP BY o.id
         HAVING SUM(CASE WHEN e.status = 10 THEN 1 ELSE 0 END) > 1

         UNION ALL

         SELECT 'BLOCK' AS severity,
                'B05_ORDER_SUCCESS_WITHOUT_SUCCESS_EXTENSION' AS code,
                CONCAT('order_id=', o.id) AS detail
         FROM pay_order o
                  JOIN pay_order_extension e
                       ON e.order_id = o.id
                           AND e.deleted = b'0'
         WHERE o.merchant_order_id = @merchant_order_id
           AND o.deleted = b'0'
         GROUP BY o.id, o.status
         HAVING o.status = 10
            AND SUM(CASE WHEN e.status = 10 THEN 1 ELSE 0 END) = 0

         UNION ALL

         SELECT 'BLOCK' AS severity,
                'B06_NOTIFY_TASK_STUCK' AS code,
                CONCAT('task_id=', t.id, ', status=', t.status, ', notify_times=', t.notify_times, '/',
                       t.max_notify_times) AS detail
         FROM pay_notify_task t
                  JOIN pay_order o
                       ON o.id = t.data_id
                           AND o.deleted = b'0'
         WHERE t.deleted = b'0'
           AND t.type = 1
           AND o.merchant_order_id = @merchant_order_id
           AND t.status IN (0, 20, 21, 22)
           AND t.create_time < DATE_SUB(NOW(), INTERVAL IFNULL(@grace_minutes, 10) MINUTE)

         UNION ALL

         SELECT 'BLOCK' AS severity,
                'B07_NOTIFY_TIMES_EXCEEDED' AS code,
                CONCAT('task_id=', t.id, ', notify_times=', t.notify_times, ', max_notify_times=', t.max_notify_times)
         FROM pay_notify_task t
                  JOIN pay_order o
                       ON o.id = t.data_id
                           AND o.deleted = b'0'
         WHERE t.deleted = b'0'
           AND t.type = 1
           AND o.merchant_order_id = @merchant_order_id
           AND t.notify_times > t.max_notify_times

         UNION ALL

         SELECT 'WARN' AS severity,
                'W01_NOTIFY_LOG_DUPLICATE_ATTEMPT' AS code,
                CONCAT('task_id=', x.task_id, ', notify_times=', x.notify_times, ', log_count=', x.cnt) AS detail
         FROM (SELECT l.task_id, l.notify_times, COUNT(*) AS cnt
               FROM pay_notify_log l
                        JOIN pay_notify_task t
                             ON t.id = l.task_id
                                 AND t.deleted = b'0'
                                 AND t.type = 1
                        JOIN pay_order o
                             ON o.id = t.data_id
                                 AND o.deleted = b'0'
                                 AND o.merchant_order_id = @merchant_order_id
               WHERE l.deleted = b'0'
               GROUP BY l.task_id, l.notify_times
               HAVING COUNT(*) > 1) x

         UNION ALL

         SELECT 'WARN' AS severity,
                'W02_STALE_WAITING_TASK' AS code,
                CONCAT('task_id=', t.id, ', next_notify_time=', DATE_FORMAT(t.next_notify_time, '%Y-%m-%d %H:%i:%s'))
         FROM pay_notify_task t
                  JOIN pay_order o
                       ON o.id = t.data_id
                           AND o.deleted = b'0'
         WHERE t.deleted = b'0'
           AND t.type = 1
           AND o.merchant_order_id = @merchant_order_id
           AND t.status = 0
           AND t.next_notify_time IS NOT NULL
           AND t.next_notify_time < DATE_SUB(NOW(), INTERVAL 5 MINUTE)

         UNION ALL

         SELECT 'WARN' AS severity,
                'W03_SUCCESS_EXTENSION_MISSING_NOTIFY_DATA' AS code,
                CONCAT('order_id=', o.id) AS detail
         FROM pay_order o
                  JOIN pay_order_extension e
                       ON e.order_id = o.id
                           AND e.deleted = b'0'
         WHERE o.merchant_order_id = @merchant_order_id
           AND o.deleted = b'0'
         GROUP BY o.id, o.status
         HAVING o.status = 10
            AND SUM(CASE
                        WHEN e.status = 10
                            AND (e.channel_notify_data IS NULL OR LENGTH(TRIM(e.channel_notify_data)) = 0)
                            THEN 1
                        ELSE 0
                 END) > 0
     ) t
ORDER BY CASE t.severity WHEN 'BLOCK' THEN 1 ELSE 2 END, t.code;
