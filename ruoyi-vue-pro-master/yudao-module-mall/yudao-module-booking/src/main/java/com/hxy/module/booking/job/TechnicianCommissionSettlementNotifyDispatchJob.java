package com.hxy.module.booking.job;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import com.hxy.module.booking.service.TechnicianCommissionSettlementService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 技师佣金结算通知派发任务
 */
@Component
public class TechnicianCommissionSettlementNotifyDispatchJob implements JobHandler {

    @Resource
    private TechnicianCommissionSettlementService settlementService;

    @Override
    @TenantJob
    public String execute(String param) {
        int count = settlementService.dispatchPendingNotifyOutbox(parseLimit(param));
        return String.format("派发佣金结算通知 %s 条", count);
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

