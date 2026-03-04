package cn.iocoder.yudao.module.trade.job.aftersale;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleReviewTicketService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 售后人工复核工单 SLA 预警任务
 */
@Component
public class AfterSaleReviewTicketWarnJob implements JobHandler {

    private static final String CONFIG_KEY_JOB_DEFAULT_LIMIT = "hxy.trade.review-ticket.sla.warn.job.batch-limit.default";
    private static final String CONFIG_KEY_JOB_MAX_LIMIT = "hxy.trade.review-ticket.sla.warn.job.batch-limit.max";
    private static final int DEFAULT_LIMIT = 200;
    private static final int DEFAULT_MAX_LIMIT = 1000;

    @Resource
    private AfterSaleReviewTicketService afterSaleReviewTicketService;
    @Resource
    private ConfigApi configApi;

    @Override
    @TenantJob
    public String execute(String param) {
        Integer limit = parseLimit(param);
        int count = afterSaleReviewTicketService.warnNearDeadlinePendingTickets(limit);
        return String.format("预警临近超时人工复核工单 %s 条", count);
    }

    private Integer parseLimit(String param) {
        int maxLimit = resolveConfigInt(CONFIG_KEY_JOB_MAX_LIMIT, DEFAULT_MAX_LIMIT, 1, 5000);
        int defaultLimit = resolveConfigInt(CONFIG_KEY_JOB_DEFAULT_LIMIT, DEFAULT_LIMIT, 1, maxLimit);
        if (StrUtil.isBlank(param) || !StrUtil.isNumeric(param.trim())) {
            return defaultLimit;
        }
        int value = Integer.parseInt(param.trim());
        if (value <= 0) {
            return defaultLimit;
        }
        return Math.min(value, maxLimit);
    }

    private int resolveConfigInt(String key, int defaultValue, int min, int max) {
        String value = configApi.getConfigValueByKey(key);
        if (StrUtil.isBlank(value) || !StrUtil.isNumeric(value.trim())) {
            return clamp(defaultValue, min, max);
        }
        return clamp(Integer.parseInt(value.trim()), min, max);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
