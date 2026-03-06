package com.hxy.module.booking.dal.mysql;

import com.hxy.module.booking.dal.dataobject.FourAccountRefundCommissionAuditRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FourAccountReconcileQueryMapper {

    @Select("SELECT COALESCE(SUM(pay_price - IFNULL(refund_price, 0)), 0) " +
            "FROM trade_order " +
            "WHERE pay_status = 1 " +
            "AND pay_time >= #{beginTime} " +
            "AND pay_time < #{endTime} " +
            "AND deleted = 0")
    Integer selectTradeNetAmount(@Param("beginTime") LocalDateTime beginTime,
                                 @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COALESCE(SUM(pay_price), 0) " +
            "FROM booking_order " +
            "WHERE status IN (1, 2, 3, 5) " +
            "AND pay_time >= #{beginTime} " +
            "AND pay_time < #{endTime} " +
            "AND deleted = 0")
    Integer selectFulfillmentAmount(@Param("beginTime") LocalDateTime beginTime,
                                    @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COALESCE(SUM(commission_amount), 0) " +
            "FROM technician_commission " +
            "WHERE status IN (0, 1) " +
            "AND create_time >= #{beginTime} " +
            "AND create_time < #{endTime} " +
            "AND deleted = 0")
    Integer selectCommissionAmount(@Param("beginTime") LocalDateTime beginTime,
                                   @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COALESCE(SUM(price), 0) " +
            "FROM trade_brokerage_record " +
            "WHERE biz_type = 1 " +
            "AND status IN (0, 1) " +
            "AND create_time >= #{beginTime} " +
            "AND create_time < #{endTime} " +
            "AND deleted = 0")
    Integer selectSplitAmount(@Param("beginTime") LocalDateTime beginTime,
                              @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT",
            "  t.id AS orderId,",
            "  t.no AS tradeOrderNo,",
            "  t.user_id AS userId,",
            "  t.pay_time AS payTime,",
            "  COALESCE(t.refund_price, 0) AS refundPrice,",
            "  COALESCE(c.settled_positive_amount, 0) AS settledCommissionAmount,",
            "  COALESCE(c.reversal_active_amount_abs, 0) AS reversalCommissionAmountAbs,",
            "  COALESCE(c.active_amount, 0) AS activeCommissionAmount",
            "FROM trade_order t",
            "LEFT JOIN (",
            "  SELECT",
            "    order_id,",
            "    SUM(CASE WHEN status = 1 AND commission_amount &gt; 0 THEN commission_amount ELSE 0 END) AS settled_positive_amount,",
            "    SUM(CASE WHEN biz_type = 'ORDER_CANCEL_REVERSAL' AND status IN (0, 1) AND commission_amount &lt; 0 THEN ABS(commission_amount) ELSE 0 END) AS reversal_active_amount_abs,",
            "    SUM(CASE WHEN status IN (0, 1) THEN commission_amount ELSE 0 END) AS active_amount",
            "  FROM technician_commission",
            "  WHERE deleted = 0",
            "  GROUP BY order_id",
            ") c ON c.order_id = t.id",
            "WHERE t.deleted = 0",
            "  AND t.pay_status = 1",
            "  AND t.pay_time IS NOT NULL",
            "  <if test='beginTime != null'> AND t.pay_time &gt;= #{beginTime} </if>",
            "  <if test='endTime != null'> AND t.pay_time &lt; #{endTime} </if>",
            "ORDER BY t.pay_time DESC, t.id DESC",
            "</script>"
    })
    List<FourAccountRefundCommissionAuditRow> selectRefundCommissionAuditCandidates(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime);
}
