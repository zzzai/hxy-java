package cn.iocoder.yudao.module.trade.framework.aftersale.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 售后退款风控规则配置
 */
@ConfigurationProperties(prefix = "yudao.trade.after-sale.refund-rule")
@Data
public class TradeAfterSaleRefundRuleProperties {

    /**
     * 是否启用退款分层路由
     */
    private Boolean enabled = Boolean.TRUE;
    /**
     * 自动退款金额上限（单位：分）
     */
    private Integer autoRefundMaxPrice = 5000;
    /**
     * 用户当天售后申请次数阈值（大于阈值进入人工复核）
     */
    private Integer userDailyApplyLimit = 3;
    /**
     * 退款黑名单用户编号
     */
    private Set<Long> blacklistUserIds = new LinkedHashSet<>();
    /**
     * 可疑订单关键字，命中则进入人工复核
     */
    private Set<String> suspiciousOrderKeywords = new LinkedHashSet<>();

}
