package com.hxy.module.booking.dal.mysql;

import com.hxy.module.booking.dal.dataobject.BookingRefundRepairCandidateDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookingRefundReconcileQueryMapper {

    @Select({
            "<script>",
            "SELECT",
            "  bo.id AS orderId,",
            "  pr.id AS payRefundId,",
            "  pr.merchant_refund_id AS merchantRefundId",
            "FROM booking_order bo",
            "INNER JOIN pay_refund pr",
            "  ON pr.deleted = 0",
            " AND pr.status = #{successStatus}",
            " AND pr.merchant_order_id = CAST(bo.id AS CHAR)",
            "WHERE bo.deleted = 0",
            "  AND (bo.status != #{refundedStatus} OR bo.pay_refund_id IS NULL OR bo.refund_time IS NULL)",
            "ORDER BY pr.id ASC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<BookingRefundRepairCandidateDO> selectRepairCandidates(@Param("successStatus") Integer successStatus,
                                                                @Param("refundedStatus") Integer refundedStatus,
                                                                @Param("limit") Integer limit);
}
