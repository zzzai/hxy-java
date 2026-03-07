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
 * booking 退款重放工单同步审计台账
 */
@TableName("hxy_booking_refund_replay_ticket_sync_log")
@KeySequence("hxy_booking_refund_replay_ticket_sync_log_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingRefundReplayTicketSyncLogDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 批次号
     */
    private String runId;

    /**
     * 退款回调台账ID
     */
    private Long notifyLogId;

    /**
     * 工单来源号
     */
    private String sourceBizNo;

    /**
     * 工单ID
     */
    private Long ticketId;

    /**
     * 同步状态：SUCCESS/SKIP/FAIL
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
     * 操作人
     */
    private String operator;

    /**
     * 同步时间
     */
    private LocalDateTime syncTime;
}
