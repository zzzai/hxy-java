package com.hxy.module.booking.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 四账对账快照 DO
 */
@TableName("hxy_four_account_reconcile")
@KeySequence("hxy_four_account_reconcile_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FourAccountReconcileDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 对账流水号
     */
    private String reconcileNo;
    /**
     * 业务日期
     */
    private LocalDate bizDate;
    /**
     * 交易账净额（分）
     */
    private Integer tradeAmount;
    /**
     * 履约账金额（分）
     */
    private Integer fulfillmentAmount;
    /**
     * 提成账金额（分）
     */
    private Integer commissionAmount;
    /**
     * 分账账金额（分）
     */
    private Integer splitAmount;
    /**
     * 差额：交易 - 履约（分）
     */
    private Integer tradeMinusFulfillment;
    /**
     * 差额：交易 - (提成 + 分账)（分）
     */
    private Integer tradeMinusCommissionSplit;
    /**
     * 对账状态
     */
    private Integer status;
    /**
     * 问题数量
     */
    private Integer issueCount;
    /**
     * 问题编码（逗号分隔）
     */
    private String issueCodes;
    /**
     * 问题明细 JSON
     */
    private String issueDetailJson;
    /**
     * 触发来源
     */
    private String source;
    /**
     * 操作人
     */
    private String operator;
    /**
     * 对账执行时间
     */
    private LocalDateTime reconciledAt;
}

