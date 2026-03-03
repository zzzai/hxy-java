package cn.iocoder.yudao.module.trade.service.aftersale;

/**
 * 售后工单路由解析器
 */
public interface AfterSaleReviewTicketRouteProvider {

    /**
     * 解析工单路由策略
     *
     * @param ticketType       工单类型
     * @param preferredSeverity 期望严重级别（可空）
     * @param ruleCode         命中规则编码（可空）
     * @return 路由
     */
    ReviewTicketRoute resolve(Integer ticketType, String preferredSeverity, String ruleCode);

}
