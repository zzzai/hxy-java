-- 支付日对账模板（D3/D5）
-- 用途：对齐订单表(eb_store_order)与微信支付流水表(eb_wechat_pay_info)
-- D5联动：退款成功订单自动消差（A3场景）
-- 注意：先在测试库验证，再用于生产

-- 1) 设置对账时间窗口（左闭右开）
SET @start_time = '2026-02-15 00:00:00';
SET @end_time   = '2026-02-16 00:00:00';

-- 2) 订单侧支付明细（按微信支付）
WITH order_paid AS (
    SELECT
        so.id,
        so.order_id,
        so.out_trade_no,
        so.uid,
        so.store_id,
        so.pay_type,
        so.is_channel,
        so.paid,
        so.pay_time,
        so.pay_price,
        IFNULL(so.refund_status, 0) AS refund_status,
        CAST(ROUND(so.pay_price * 100, 0) AS SIGNED) AS pay_price_cent
    FROM eb_store_order so
    WHERE so.pay_type = 'weixin'
      AND so.pay_time >= @start_time
      AND so.pay_time < @end_time
),
pay_info AS (
    SELECT
        wpi.id,
        wpi.out_trade_no,
        wpi.app_id,
        wpi.mch_id,
        wpi.total_fee,
        wpi.trade_state,
        wpi.transaction_id,
        wpi.time_end
    FROM eb_wechat_pay_info wpi
    WHERE wpi.out_trade_no IS NOT NULL
),
refund_success AS (
    SELECT
        sos.oid,
        1 AS has_refund_success_log
    FROM eb_store_order_status sos
    WHERE sos.change_type = 'refund_price'
      AND sos.change_message LIKE '%成功%'
    GROUP BY sos.oid
)

-- 3) 主对账结果（差异清单）
SELECT
    op.order_id,
    op.out_trade_no,
    op.store_id,
    op.pay_time,
    op.pay_price_cent AS order_total_fee,
    pi.total_fee      AS wx_total_fee,
    op.paid           AS order_paid_flag,
    op.refund_status  AS order_refund_status,
    IFNULL(rs.has_refund_success_log, 0) AS refund_success_log,
    pi.trade_state    AS wx_trade_state,
    pi.mch_id,
    pi.app_id,
    pi.transaction_id,
    CASE
        WHEN pi.out_trade_no IS NULL THEN 'A1_订单有支付记录_但微信流水缺失'
        WHEN op.pay_price_cent <> IFNULL(pi.total_fee, -1) THEN 'A2_金额不一致'
        WHEN op.paid = 1 AND IFNULL(pi.trade_state, '') <> 'SUCCESS' THEN 'A3_订单已支付_微信非SUCCESS'
        WHEN op.paid = 0 AND IFNULL(pi.trade_state, '') = 'SUCCESS' THEN 'A4_微信SUCCESS_订单未支付'
        ELSE 'OK'
    END AS diff_type,
    CASE
        WHEN op.paid = 1
          AND IFNULL(pi.trade_state, '') <> 'SUCCESS'
          AND op.refund_status = 2
          AND IFNULL(rs.has_refund_success_log, 0) = 1
          AND IFNULL(pi.trade_state, '') IN ('REFUND', 'SUCCESS')
        THEN 1
        ELSE 0
    END AS auto_clear_by_refund,
    CASE
        WHEN op.paid = 1
          AND IFNULL(pi.trade_state, '') <> 'SUCCESS'
          AND op.refund_status = 2
          AND IFNULL(rs.has_refund_success_log, 0) = 1
          AND IFNULL(pi.trade_state, '') IN ('REFUND', 'SUCCESS')
        THEN 'C1_退款成功自动消差'
        ELSE ''
    END AS auto_clear_reason
FROM order_paid op
LEFT JOIN pay_info pi
       ON pi.out_trade_no = op.out_trade_no
LEFT JOIN refund_success rs
       ON rs.oid = op.id
WHERE pi.out_trade_no IS NULL
   OR op.pay_price_cent <> IFNULL(pi.total_fee, -1)
   OR (op.paid = 1 AND IFNULL(pi.trade_state, '') <> 'SUCCESS')
   OR (op.paid = 0 AND IFNULL(pi.trade_state, '') = 'SUCCESS')
ORDER BY op.pay_time, op.order_id;

-- 4) 微信侧孤儿流水（微信成功，但订单不存在）
SELECT
    pi.out_trade_no,
    pi.transaction_id,
    pi.total_fee,
    pi.trade_state,
    pi.mch_id,
    pi.app_id,
    pi.time_end
FROM pay_info pi
LEFT JOIN eb_store_order so
       ON so.out_trade_no = pi.out_trade_no
WHERE so.id IS NULL
  AND IFNULL(pi.trade_state, '') = 'SUCCESS'
ORDER BY pi.time_end;
