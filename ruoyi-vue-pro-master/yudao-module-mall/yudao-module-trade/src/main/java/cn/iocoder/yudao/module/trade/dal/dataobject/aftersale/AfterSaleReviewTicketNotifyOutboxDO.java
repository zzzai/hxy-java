package cn.iocoder.yudao.module.trade.dal.dataobject.aftersale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 售后人工复核工单通知出站 DO
 */
@TableName("trade_after_sale_review_ticket_notify_outbox")
@KeySequence("trade_after_sale_review_ticket_notify_outbox_seq")
@Data
public class AfterSaleReviewTicketNotifyOutboxDO extends BaseDO {

    /**
     * 主键
     */
    private Long id;
    /**
     * 工单 ID
     */
    private Long ticketId;
    /**
     * 通知类型：SLA_WARN / SLA_ESCALATE
     */
    private String notifyType;
    /**
     * 通知渠道：IN_APP
     */
    private String channel;
    /**
     * 严重级别：P0 / P1 / P2
     */
    private String severity;
    /**
     * 升级对象快照
     */
    private String escalateTo;
    /**
     * 幂等业务键
     */
    private String bizKey;
    /**
     * 状态：0 待发送 1 已发送 2 发送失败
     */
    private Integer status;
    /**
     * 重试次数
     */
    private Integer retryCount;
    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryTime;
    /**
     * 发送成功时间
     */
    private LocalDateTime sentTime;
    /**
     * 最后错误信息
     */
    private String lastErrorMsg;
    /**
     * 最近审计动作编码
     */
    private String lastActionCode;
    /**
     * 最近审计业务号
     */
    private String lastActionBizNo;
    /**
     * 最近审计动作时间
     */
    private LocalDateTime lastActionTime;
}
