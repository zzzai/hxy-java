package com.hxy.module.booking.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * booking 退款回调重放批次明细台账
 */
@TableName("hxy_booking_refund_replay_run_detail")
@KeySequence("hxy_booking_refund_replay_run_detail_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingRefundReplayRunDetailDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 批次号
     */
    private String runId;

    /**
     * 批次台账ID
     */
    private Long runLogId;

    /**
     * 退款回调台账ID
     */
    private Long notifyLogId;

    /**
     * 预约订单ID
     */
    private Long orderId;

    /**
     * 支付退款单ID
     */
    private Long payRefundId;

    /**
     * 重放结果：SUCCESS/SKIP/FAIL
     */
    private String resultStatus;

    /**
     * 结果码
     */
    private String resultCode;

    /**
     * 结果消息
     */
    private String resultMsg;

    /**
     * 告警标签（如 FOUR_ACCOUNT_REFRESH_WARN）
     */
    private String warningTag;
}
