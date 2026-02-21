package cn.iocoder.yudao.module.trade.framework.order.config;

import cn.iocoder.yudao.module.trade.framework.aftersale.config.TradeAfterSaleRefundRuleProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author LeeYan9
 * @since 2022-09-15
 */
@Configuration
@EnableConfigurationProperties({TradeOrderProperties.class, TradeAfterSaleRefundRuleProperties.class})
public class TradeOrderConfig {
}
