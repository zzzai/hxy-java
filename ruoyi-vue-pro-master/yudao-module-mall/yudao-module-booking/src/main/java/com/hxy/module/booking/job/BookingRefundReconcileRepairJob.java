package com.hxy.module.booking.job;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import com.hxy.module.booking.service.BookingRefundNotifyLogService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * booking 退款回调补偿修复任务
 */
@Component
public class BookingRefundReconcileRepairJob implements JobHandler {

    private static final int DEFAULT_LIMIT = 200;

    @Resource
    private BookingRefundNotifyLogService refundNotifyLogService;

    @Override
    @TenantJob
    public String execute(String param) {
        Integer limit = parseLimit(param);
        int repaired = refundNotifyLogService.reconcileRefundedOrders(limit);
        return String.format("booking退款补偿修复完成 repaired=%d limit=%d", repaired, limit);
    }

    private Integer parseLimit(String param) {
        if (StrUtil.isBlank(param)) {
            return DEFAULT_LIMIT;
        }
        String trimmed = param.trim();
        if (NumberUtil.isInteger(trimmed)) {
            return Integer.parseInt(trimmed);
        }
        String[] parts = trimmed.split("=");
        if (parts.length == 2 && NumberUtil.isInteger(parts[1].trim())) {
            return Integer.parseInt(parts[1].trim());
        }
        return DEFAULT_LIMIT;
    }
}
