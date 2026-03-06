package com.hxy.module.booking.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * booking 退款回调台账 DO
 */
@TableName("hxy_booking_refund_notify_log")
@KeySequence("hxy_booking_refund_notify_log_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingRefundNotifyLogDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 预约订单ID
     */
    private Long orderId;

    /**
     * 商户退款单号
     */
    private String merchantRefundId;

    /**
     * 支付退款单ID
     */
    private Long payRefundId;

    /**
     * 状态：success / fail
     */
    private String status;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 原始请求载荷
     */
    private String rawPayload;

    /**
     * 重放次数
     */
    private Integer retryCount;

    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryTime;

    /**
     * 最近重放操作人
     */
    private String lastReplayOperator;

    /**
     * 最近重放时间
     */
    private LocalDateTime lastReplayTime;

    /**
     * 最近重放结果：SUCCESS / SKIP / FAIL
     */
    private String lastReplayResult;

    /**
     * 最近重放备注
     */
    private String lastReplayRemark;
}
