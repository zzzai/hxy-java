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
 * 技师佣金结算通知出站表
 */
@TableName("technician_commission_settlement_notify_outbox")
@KeySequence("technician_commission_settlement_notify_outbox_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TechnicianCommissionSettlementNotifyOutboxDO extends BaseDO {

    @TableId
    private Long id;

    /** 结算单ID */
    private Long settlementId;

    /** 通知类型：P1_WARN / P0_ESCALATE */
    private String notifyType;

    /** 通知渠道：IN_APP */
    private String channel;

    /** 优先级：P1 / P0 */
    private String severity;

    /** 幂等业务键 */
    private String bizKey;

    /** 状态：0待发送 1已发送 2发送失败 */
    private Integer status;

    /** 重试次数 */
    private Integer retryCount;

    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;

    /** 发送成功时间 */
    private LocalDateTime sentTime;

    /** 最后错误信息 */
    private String lastErrorMsg;

    /** 最近审计动作编码 */
    private String lastActionCode;

    /** 最近审计业务号 */
    private String lastActionBizNo;

    /** 最近审计动作时间 */
    private LocalDateTime lastActionTime;
}
