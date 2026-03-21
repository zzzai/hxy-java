package com.hxy.module.booking.job;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import com.hxy.module.booking.service.BookingReviewNotifyOutboxService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BookingReviewManagerTodoSlaReminderJob implements JobHandler {

    @Resource
    private BookingReviewNotifyOutboxService bookingReviewNotifyOutboxService;

    @Override
    @TenantJob
    public String execute(String param) {
        int count = bookingReviewNotifyOutboxService.createManagerTodoSlaReminderOutbox(parseLimit(param));
        return String.format("创建预约评价 SLA 提醒 %s 条", count);
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
