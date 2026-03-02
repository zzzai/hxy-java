package cn.iocoder.yudao.module.trade.job.aftersale;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleReviewTicketService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 售后人工复核工单逾期升级任务
 *
 * @author HXY
 */
@Component
public class AfterSaleReviewTicketEscalationJob implements JobHandler {

    @Resource
    private AfterSaleReviewTicketService afterSaleReviewTicketService;

    @Override
    @TenantJob
    public String execute(String param) {
        Integer limit = parseLimit(param);
        int count = afterSaleReviewTicketService.escalateOverduePendingTickets(limit);
        return String.format("升级逾期人工复核工单 %s 条", count);
    }

    private Integer parseLimit(String param) {
        if (StrUtil.isBlank(param) || !StrUtil.isNumeric(param.trim())) {
            return 200;
        }
        int value = Integer.parseInt(param.trim());
        if (value <= 0) {
            return 200;
        }
        return Math.min(value, 1000);
    }

}
