package cn.iocoder.yudao.module.trade.controller.admin.aftersale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.rule.AfterSaleRefundRuleRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.rule.AfterSaleRefundRuleSaveReqVO;
import cn.iocoder.yudao.module.trade.convert.aftersale.AfterSaleRefundRuleConvert;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleRefundRuleConfigDO;
import cn.iocoder.yudao.module.trade.framework.aftersale.config.TradeAfterSaleRefundRuleProperties;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleRefundRuleConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 售后退款规则
 *
 * @author HXY
 */
@Tag(name = "管理后台 - 售后退款规则")
@RestController
@RequestMapping("/trade/after-sale/refund-rule")
@Validated
public class AfterSaleRefundRuleController {

    @Resource
    private AfterSaleRefundRuleConfigService afterSaleRefundRuleConfigService;
    @Resource
    private TradeAfterSaleRefundRuleProperties refundRuleProperties;

    @GetMapping("/get")
    @Operation(summary = "获取当前生效退款规则")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:query')")
    public CommonResult<AfterSaleRefundRuleRespVO> getRule() {
        AfterSaleRefundRuleConfigDO dbRule = afterSaleRefundRuleConfigService.getLatestDbRule();
        if (dbRule != null) {
            AfterSaleRefundRuleRespVO respVO = AfterSaleRefundRuleConvert.INSTANCE.convert(dbRule);
            respVO.setSource("DB");
            return success(respVO);
        }
        AfterSaleRefundRuleRespVO yamlRule = new AfterSaleRefundRuleRespVO();
        yamlRule.setEnabled(refundRuleProperties.getEnabled());
        yamlRule.setAutoRefundMaxPrice(refundRuleProperties.getAutoRefundMaxPrice());
        yamlRule.setUserDailyApplyLimit(refundRuleProperties.getUserDailyApplyLimit());
        yamlRule.setBlacklistUserIds(refundRuleProperties.getBlacklistUserIds().stream().collect(Collectors.toList()));
        yamlRule.setSuspiciousOrderKeywords(refundRuleProperties.getSuspiciousOrderKeywords().stream().collect(Collectors.toList()));
        yamlRule.setRuleVersion("yaml-default");
        yamlRule.setRemark("当前无 DB 规则，使用 YAML 兜底规则");
        yamlRule.setSource("YAML");
        return success(yamlRule);
    }

    @PutMapping("/save")
    @Operation(summary = "保存退款规则（版本化新增）")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CommonResult<Long> saveRule(@Valid @RequestBody AfterSaleRefundRuleSaveReqVO reqVO) {
        return success(afterSaleRefundRuleConfigService.saveRule(reqVO));
    }

}
