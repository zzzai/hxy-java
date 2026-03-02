package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.rule.AfterSaleRefundRuleSaveReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleRefundRuleConfigDO;

import javax.validation.Valid;

/**
 * 售后退款规则配置 Service
 *
 * @author HXY
 */
public interface AfterSaleRefundRuleConfigService {

    /**
     * 获取最新 DB 规则
     *
     * @return 最新规则；若不存在返回 null
     */
    AfterSaleRefundRuleConfigDO getLatestDbRule();

    /**
     * 保存规则（版本化新增）
     *
     * @param reqVO 保存请求
     * @return 新增 ID
     */
    Long saveRule(@Valid AfterSaleRefundRuleSaveReqVO reqVO);

}
