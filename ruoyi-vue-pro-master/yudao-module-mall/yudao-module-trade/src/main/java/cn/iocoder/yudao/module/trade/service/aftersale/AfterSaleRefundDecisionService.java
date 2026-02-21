package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleRefundDecisionBO;

/**
 * 售后退款风控决策服务
 */
public interface AfterSaleRefundDecisionService {

    /**
     * 评估售后退款是否可自动执行
     *
     * @param afterSale 售后单
     * @return 决策结果
     */
    AfterSaleRefundDecisionBO evaluate(AfterSaleDO afterSale);

    /**
     * 退款执行前校验并记录审计日志
     *
     * @param operatorId   操作人
     * @param operatorType 操作人类型
     * @param afterSale    售后单
     * @param forcePass    是否强制执行
     * @return 决策结果
     */
    AfterSaleRefundDecisionBO checkAndAuditForExecution(Long operatorId, Integer operatorType,
                                                        AfterSaleDO afterSale, boolean forcePass);

    /**
     * 记录退款风控决策审计日志
     *
     * @param operatorId   操作人
     * @param operatorType 操作人类型
     * @param afterSale    售后单
     * @param decision     决策结果
     * @param forcePass    是否强制执行
     */
    void auditDecision(Long operatorId, Integer operatorType, AfterSaleDO afterSale,
                       AfterSaleRefundDecisionBO decision, boolean forcePass);

}
