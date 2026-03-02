package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleRefundRuleConfigDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleRefundRuleConfigMapper;
import cn.iocoder.yudao.module.trade.framework.aftersale.config.TradeAfterSaleRefundRuleProperties;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleRefundRuleSnapshotBO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 售后退款风控规则解析器
 *
 * 解析优先级：DB 规则表 > YAML 配置。
 *
 * @author HXY
 */
@Component
public class AfterSaleRefundRuleResolver {

    @Resource
    private AfterSaleRefundRuleConfigMapper afterSaleRefundRuleConfigMapper;
    @Resource
    private TradeAfterSaleRefundRuleProperties refundRuleProperties;

    public AfterSaleRefundRuleSnapshotBO resolve() {
        AfterSaleRefundRuleConfigDO dbRule = afterSaleRefundRuleConfigMapper.selectLatest();
        if (dbRule != null) {
            return new AfterSaleRefundRuleSnapshotBO(
                    dbRule.getEnabled(),
                    dbRule.getAutoRefundMaxPrice(),
                    dbRule.getUserDailyApplyLimit(),
                    normalizeLongSet(dbRule.getBlacklistUserIds()),
                    normalizeStringSet(dbRule.getSuspiciousOrderKeywords()),
                    "DB");
        }
        return new AfterSaleRefundRuleSnapshotBO(
                refundRuleProperties.getEnabled(),
                refundRuleProperties.getAutoRefundMaxPrice(),
                refundRuleProperties.getUserDailyApplyLimit(),
                normalizeLongSet(refundRuleProperties.getBlacklistUserIds()),
                normalizeStringSet(refundRuleProperties.getSuspiciousOrderKeywords()),
                "YAML");
    }

    private Set<Long> normalizeLongSet(Iterable<Long> source) {
        Set<Long> results = new LinkedHashSet<>();
        if (source == null) {
            return results;
        }
        for (Long value : source) {
            if (value != null && value > 0) {
                results.add(value);
            }
        }
        return results;
    }

    private Set<String> normalizeStringSet(Iterable<String> source) {
        Set<String> results = new LinkedHashSet<>();
        if (source == null) {
            return results;
        }
        for (String value : source) {
            if (StrUtil.isNotBlank(value)) {
                results.add(value.trim());
            }
        }
        return results;
    }

}
