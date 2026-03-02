package com.hxy.module.booking.job;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import com.hxy.module.booking.service.TechnicianCommissionSettlementService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 技师佣金结算单 SLA 预警任务
 */
@Component
public class TechnicianCommissionSettlementSlaWarnJob implements JobHandler {

    @Resource
    private TechnicianCommissionSettlementService settlementService;

    @Override
    @TenantJob
    public String execute(String param) {
        JobParam jobParam = parseParam(param);
        int warnCount = settlementService.warnNearDeadlinePending(jobParam.getLeadMinutes(), jobParam.getLimit());
        int escalateCount = settlementService.escalateOverduePendingToP0(jobParam.getDelayMinutes(), jobParam.getLimit());
        return String.format("预警待审核佣金结算单 %s 条，升级P0 %s 条", warnCount, escalateCount);
    }

    private JobParam parseParam(String param) {
        int defaultLeadMinutes = 30;
        int defaultDelayMinutes = 30;
        int defaultLimit = 200;
        if (StrUtil.isBlank(param)) {
            return new JobParam(defaultLeadMinutes, defaultDelayMinutes, defaultLimit);
        }
        String[] parts = param.trim().split(",");
        if (parts.length != 3
                || !StrUtil.isNumeric(parts[0].trim())
                || !StrUtil.isNumeric(parts[1].trim())
                || !StrUtil.isNumeric(parts[2].trim())) {
            return new JobParam(defaultLeadMinutes, defaultDelayMinutes, defaultLimit);
        }
        int leadMinutes = Integer.parseInt(parts[0].trim());
        int delayMinutes = Integer.parseInt(parts[1].trim());
        int limit = Integer.parseInt(parts[2].trim());
        if (leadMinutes <= 0) {
            leadMinutes = defaultLeadMinutes;
        }
        if (delayMinutes <= 0) {
            delayMinutes = defaultDelayMinutes;
        }
        if (limit <= 0) {
            limit = defaultLimit;
        }
        return new JobParam(Math.min(leadMinutes, 1440), Math.min(delayMinutes, 1440), Math.min(limit, 1000));
    }

    private static class JobParam {
        private final Integer leadMinutes;
        private final Integer delayMinutes;
        private final Integer limit;

        private JobParam(Integer leadMinutes, Integer delayMinutes, Integer limit) {
            this.leadMinutes = leadMinutes;
            this.delayMinutes = delayMinutes;
            this.limit = limit;
        }

        public Integer getLeadMinutes() {
            return leadMinutes;
        }

        public Integer getDelayMinutes() {
            return delayMinutes;
        }

        public Integer getLimit() {
            return limit;
        }
    }
}
