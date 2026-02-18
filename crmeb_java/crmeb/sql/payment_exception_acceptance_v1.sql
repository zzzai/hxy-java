-- 支付异常场景验收 SQL（v1）
-- 用途：为 payment_exception_acceptance.sh 提供可复核的 SQL 基线。
-- 说明：执行前可按需调整窗口参数。

SET @window_hours = 72;
SET @order_timeout_hours = 2;
SET @refund_timeout_minutes = 30;
SET @start_time = DATE_SUB(NOW(), INTERVAL @window_hours HOUR);
SET @overdue_unpaid_before = DATE_SUB(NOW(), INTERVAL @order_timeout_hours HOUR);
SET @refund_timeout_before = DATE_SUB(NOW(), INTERVAL @refund_timeout_minutes MINUTE);

-- P01: 已支付但缺少 pay_success 日志（期望 0 行）
SELECT so.order_id, so.out_trade_no, so.pay_time
FROM eb_store_order so
WHERE so.pay_type='weixin'
  AND so.paid=1
  AND so.pay_time >= @start_time
  AND NOT EXISTS (
    SELECT 1 FROM eb_store_order_status s
    WHERE s.oid = so.id
      AND s.change_type='pay_success'
  )
ORDER BY so.pay_time DESC;

-- P02: 已支付但缺少微信流水（期望 0 行）
SELECT so.order_id, so.out_trade_no, so.pay_time
FROM eb_store_order so
LEFT JOIN (
  SELECT out_trade_no, MAX(id) AS max_id
  FROM eb_wechat_pay_info
  GROUP BY out_trade_no
) lp ON lp.out_trade_no = so.out_trade_no
WHERE so.pay_type='weixin'
  AND so.paid=1
  AND so.pay_time >= @start_time
  AND lp.max_id IS NULL
ORDER BY so.pay_time DESC;

-- P03: 微信 SUCCESS 但本地订单未收敛（期望 0 行）
SELECT p.out_trade_no, p.transaction_id, p.trade_state, p.time_end, IFNULL(so.order_id,'') AS order_id, IFNULL(so.paid,0) AS paid
FROM eb_wechat_pay_info p
LEFT JOIN eb_store_order so ON so.out_trade_no = p.out_trade_no
WHERE IFNULL(p.trade_state,'')='SUCCESS'
  AND (so.id IS NULL OR so.paid <> 1)
ORDER BY p.id DESC;

-- P04: 已支付订单微信侧 trade_state 异常（建议 0 行）
SELECT so.order_id, so.out_trade_no, IFNULL(pi.trade_state,'') AS trade_state, pi.transaction_id, so.pay_time
FROM eb_store_order so
JOIN (
  SELECT out_trade_no, MAX(id) AS max_id
  FROM eb_wechat_pay_info
  GROUP BY out_trade_no
) lp ON lp.out_trade_no = so.out_trade_no
JOIN eb_wechat_pay_info pi ON pi.id = lp.max_id
WHERE so.pay_type='weixin'
  AND so.paid=1
  AND so.pay_time >= @start_time
  AND IFNULL(pi.trade_state,'') NOT IN ('SUCCESS', 'REFUND')
ORDER BY so.pay_time DESC;

-- P05: pay_success 重复日志（期望 0 行）
SELECT so.order_id, so.out_trade_no, COUNT(*) AS cnt
FROM eb_store_order_status s
JOIN eb_store_order so ON so.id = s.oid
WHERE s.change_type='pay_success'
  AND s.create_time >= @start_time
GROUP BY s.oid, so.order_id, so.out_trade_no
HAVING COUNT(*) > 1
ORDER BY cnt DESC;

-- P06: refund_success 重复日志（期望 0 行）
SELECT so.order_id, so.out_trade_no, COUNT(*) AS cnt
FROM eb_store_order_status s
JOIN eb_store_order so ON so.id = s.oid
WHERE s.change_type='refund_price'
  AND s.change_message LIKE '%成功%'
  AND s.create_time >= @start_time
GROUP BY s.oid, so.order_id, so.out_trade_no
HAVING COUNT(*) > 1
ORDER BY cnt DESC;

-- P07: 同一 out_trade_no 对应多笔已支付订单（期望 0 行）
SELECT out_trade_no, COUNT(*) AS order_count, GROUP_CONCAT(order_id ORDER BY id ASC SEPARATOR ',') AS order_ids
FROM eb_store_order
WHERE pay_type='weixin'
  AND paid=1
  AND out_trade_no IS NOT NULL
  AND out_trade_no <> ''
  AND pay_time >= @start_time
GROUP BY out_trade_no
HAVING COUNT(*) > 1
ORDER BY order_count DESC;

-- P08: 同一 transaction_id 多次出现（期望 0 行）
SELECT transaction_id, COUNT(*) AS flow_count, GROUP_CONCAT(out_trade_no ORDER BY id ASC SEPARATOR ',') AS out_trade_nos
FROM eb_wechat_pay_info
WHERE transaction_id IS NOT NULL
  AND transaction_id <> ''
GROUP BY transaction_id
HAVING COUNT(*) > 1
ORDER BY flow_count DESC;

-- P09: 超时未支付订单未取消（期望 0 行）
SELECT so.order_id, so.out_trade_no, so.create_time, so.is_del, so.is_system_del
FROM eb_store_order so
WHERE so.paid=0
  AND so.is_del=0
  AND so.is_system_del=0
  AND so.create_time <= @overdue_unpaid_before
ORDER BY so.create_time ASC
LIMIT 500;

-- P10: 自动取消缺少日志（期望 0 行）
SELECT so.order_id, so.out_trade_no, so.update_time
FROM eb_store_order so
WHERE so.paid=0
  AND so.is_del=1
  AND so.is_system_del=1
  AND so.update_time >= @start_time
  AND NOT EXISTS (
    SELECT 1 FROM eb_store_order_status s
    WHERE s.oid = so.id
      AND s.change_type = 'cancel'
      AND s.change_message LIKE '%到期未支付系统自动取消%'
  )
ORDER BY so.update_time DESC;

-- P11: 自动取消后优惠券未回滚可用（期望 0 行）
SELECT so.order_id, so.out_trade_no, so.coupon_id, scu.status AS coupon_status, so.update_time
FROM eb_store_order so
JOIN eb_store_coupon_user scu ON scu.id = so.coupon_id
WHERE so.paid=0
  AND so.is_del=1
  AND so.is_system_del=1
  AND so.coupon_id > 0
  AND so.update_time >= @start_time
  AND scu.status <> 0
ORDER BY so.update_time DESC;

-- P12: 本地已取消但微信流水显示 SUCCESS（期望 0 行）
SELECT so.order_id, so.out_trade_no, so.update_time, IFNULL(pi.trade_state,'') AS trade_state, pi.transaction_id
FROM eb_store_order so
JOIN (
  SELECT out_trade_no, MAX(id) AS max_id
  FROM eb_wechat_pay_info
  WHERE out_trade_no IS NOT NULL AND out_trade_no <> ''
  GROUP BY out_trade_no
) lp ON lp.out_trade_no = so.out_trade_no
JOIN eb_wechat_pay_info pi ON pi.id = lp.max_id
WHERE so.paid=0
  AND so.is_del=1
  AND so.is_system_del=1
  AND so.update_time >= @start_time
  AND IFNULL(pi.trade_state,'')='SUCCESS'
ORDER BY so.update_time DESC;

-- P13: refund_status=2 但缺少成功退款日志（期望 0 行）
SELECT so.order_id, so.out_trade_no, so.refund_status, so.update_time
FROM eb_store_order so
WHERE so.refund_status = 2
  AND so.update_time >= @start_time
  AND NOT EXISTS (
    SELECT 1 FROM eb_store_order_status s
    WHERE s.oid = so.id
      AND s.change_type='refund_price'
      AND s.change_message LIKE '%成功%'
  )
ORDER BY so.update_time DESC;

-- P14: 有退款成功日志但订单状态未收敛为2（期望 0 行）
SELECT so.order_id, so.out_trade_no, so.refund_status, MAX(s.create_time) AS refund_log_time
FROM eb_store_order so
JOIN eb_store_order_status s ON s.oid = so.id
WHERE s.change_type='refund_price'
  AND s.change_message LIKE '%成功%'
  AND s.create_time >= @start_time
  AND so.refund_status <> 2
GROUP BY so.id, so.order_id, so.out_trade_no, so.refund_status
ORDER BY refund_log_time DESC;

-- P15: refund_status=3 超时未收敛（期望 0 行）
SELECT so.order_id, so.out_trade_no, so.refund_status, so.update_time
FROM eb_store_order so
WHERE so.refund_status = 3
  AND so.update_time <= @refund_timeout_before
ORDER BY so.update_time ASC
LIMIT 500;

-- P16: refund_status=1 长时间未推进（建议 0 行）
SELECT so.order_id, so.out_trade_no, so.refund_status, so.update_time
FROM eb_store_order so
WHERE so.refund_status = 1
  AND so.update_time <= @overdue_unpaid_before
ORDER BY so.update_time ASC
LIMIT 500;
