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
 * 预约评价通知出站表 DO
 */
@TableName("booking_review_notify_outbox")
@KeySequence("booking_review_notify_outbox_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingReviewNotifyOutboxDO extends BaseDO {

    @TableId
    private Long id;

    /** 业务类型 */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 门店ID */
    private Long storeId;

    /** 接收角色 */
    private String receiverRole;

    /** 接收账号ID */
    private Long receiverUserId;

    /** 接收账号 */
    private String receiverAccount;

    /** 通知类型 */
    private String notifyType;

    /** 通知渠道 */
    private String channel;

    /** 状态 */
    private String status;

    /** 重试次数 */
    private Integer retryCount;

    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;

    /** 发送成功时间 */
    private LocalDateTime sentTime;

    /** 最后错误信息 */
    private String lastErrorMsg;

    /** 幂等键 */
    private String idempotencyKey;

    /** 载荷快照 */
    private String payloadSnapshot;

    /** 最近动作编码 */
    private String lastActionCode;

    /** 最近动作业务号 */
    private String lastActionBizNo;

    /** 最近动作时间 */
    private LocalDateTime lastActionTime;
}
