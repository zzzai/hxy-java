package com.hxy.module.booking.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 技师佣金结算单
 */
@TableName("technician_commission_settlement")
@KeySequence("technician_commission_settlement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianCommissionSettlementDO extends BaseDO {

    @TableId
    private Long id;

    /** 结算单号 */
    private String settlementNo;

    /** 门店ID */
    private Long storeId;

    /** 技师ID */
    private Long technicianId;

    /** 结算状态 */
    private Integer status;

    /** 佣金条目数量 */
    private Integer commissionCount;

    /** 佣金总金额（分） */
    private Integer totalCommissionAmount;

    /** 提审时间 */
    private LocalDateTime reviewSubmitTime;

    /** 审核SLA截止时间 */
    private LocalDateTime reviewDeadlineTime;

    /** 是否已预警 */
    private Boolean reviewWarned;

    /** 预警时间 */
    private LocalDateTime reviewWarnTime;

    /** 是否已升级到P0 */
    private Boolean reviewEscalated;

    /** 升级时间 */
    private LocalDateTime reviewEscalateTime;

    /** 审核时间 */
    private LocalDateTime reviewedTime;

    /** 审核人 */
    private Long reviewerId;

    /** 审核备注 */
    private String reviewRemark;

    /** 驳回原因 */
    private String rejectReason;

    /** 打款时间 */
    private LocalDateTime paidTime;

    /** 打款人 */
    private Long payerId;

    /** 打款凭证号 */
    private String payVoucherNo;

    /** 打款备注 */
    private String payRemark;

    /** 备注 */
    private String remark;
}
