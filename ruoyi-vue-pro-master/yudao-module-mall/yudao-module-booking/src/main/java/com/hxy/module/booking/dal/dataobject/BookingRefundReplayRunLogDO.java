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
 * booking 退款回调重放批次运行台账
 */
@TableName("hxy_booking_refund_replay_run_log")
@KeySequence("hxy_booking_refund_replay_run_log_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingRefundReplayRunLogDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 批次号
     */
    private String runId;

    /**
     * 触发来源：MANUAL/JOB
     */
    private String triggerSource;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 是否预演
     */
    private Boolean dryRun;

    /**
     * 扫描上限
     */
    private Integer limitSize;

    /**
     * 扫描总数
     */
    private Integer scannedCount;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 跳过数
     */
    private Integer skipCount;

    /**
     * 失败数
     */
    private Integer failCount;

    /**
     * 批次状态：started/success/partial_fail/fail
     */
    private String status;

    /**
     * 批次错误/告警摘要
     */
    private String errorMsg;

    /**
     * 启动时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;
}
