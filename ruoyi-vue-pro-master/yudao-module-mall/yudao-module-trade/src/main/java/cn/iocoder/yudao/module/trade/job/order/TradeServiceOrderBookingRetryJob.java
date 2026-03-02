package cn.iocoder.yudao.module.trade.job.order;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.trade.service.order.TradeServiceOrderService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 服务履约单预约占位重试任务
 *
 * 灰度阶段用于收敛预约网关抖动导致的占位失败。
 *
 * @author HXY
 */
@Component
public class TradeServiceOrderBookingRetryJob implements JobHandler {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;

    @Resource
    private TradeServiceOrderService tradeServiceOrderService;

    @Override
    @TenantJob
    public String execute(String param) {
        Integer limit = parseLimit(param);
        int successCount = tradeServiceOrderService.retryCreateBookingPlaceholder(limit);
        return String.format("重试服务履约单预约占位成功 %s 条", successCount);
    }

    private Integer parseLimit(String param) {
        if (StrUtil.isBlank(param) || !StrUtil.isNumeric(param.trim())) {
            return DEFAULT_LIMIT;
        }
        int value = Integer.parseInt(param.trim());
        if (value <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(value, MAX_LIMIT);
    }

}
