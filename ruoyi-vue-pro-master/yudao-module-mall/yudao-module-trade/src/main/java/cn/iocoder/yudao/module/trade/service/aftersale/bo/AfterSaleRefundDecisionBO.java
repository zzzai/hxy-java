package cn.iocoder.yudao.module.trade.service.aftersale.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 售后退款风控决策结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AfterSaleRefundDecisionBO {

    /**
     * 是否可自动执行退款
     */
    private Boolean autoPass;
    /**
     * 命中的规则编码
     */
    private String ruleCode;
    /**
     * 命中的规则说明
     */
    private String reason;

    public static AfterSaleRefundDecisionBO auto(String ruleCode, String reason) {
        return new AfterSaleRefundDecisionBO(Boolean.TRUE, ruleCode, reason);
    }

    public static AfterSaleRefundDecisionBO manual(String ruleCode, String reason) {
        return new AfterSaleRefundDecisionBO(Boolean.FALSE, ruleCode, reason);
    }

}
