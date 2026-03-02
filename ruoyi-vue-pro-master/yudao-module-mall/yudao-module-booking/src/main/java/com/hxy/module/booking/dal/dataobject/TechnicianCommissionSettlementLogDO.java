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
 * 技师佣金结算单操作日志
 */
@TableName("technician_commission_settlement_log")
@KeySequence("technician_commission_settlement_log_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TechnicianCommissionSettlementLogDO extends BaseDO {

    @TableId
    private Long id;

    /** 结算单ID */
    private Long settlementId;

    /** 动作 */
    private String action;

    /** 源状态 */
    private Integer fromStatus;

    /** 目标状态 */
    private Integer toStatus;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人类型 */
    private String operatorType;

    /** 操作备注 */
    private String operateRemark;

    /** 操作时间 */
    private LocalDateTime actionTime;
}

