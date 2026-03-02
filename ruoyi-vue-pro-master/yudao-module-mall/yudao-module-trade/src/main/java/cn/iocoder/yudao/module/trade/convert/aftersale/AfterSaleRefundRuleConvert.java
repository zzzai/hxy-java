package cn.iocoder.yudao.module.trade.convert.aftersale;

import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.rule.AfterSaleRefundRuleRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.rule.AfterSaleRefundRuleSaveReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleRefundRuleConfigDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 售后退款规则 Convert
 *
 * @author HXY
 */
@Mapper
public interface AfterSaleRefundRuleConvert {

    AfterSaleRefundRuleConvert INSTANCE = Mappers.getMapper(AfterSaleRefundRuleConvert.class);

    AfterSaleRefundRuleConfigDO convert(AfterSaleRefundRuleSaveReqVO bean);

    AfterSaleRefundRuleRespVO convert(AfterSaleRefundRuleConfigDO bean);

}
