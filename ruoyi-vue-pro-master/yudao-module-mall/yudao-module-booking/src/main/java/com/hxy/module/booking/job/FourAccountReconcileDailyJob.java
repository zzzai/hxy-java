package com.hxy.module.booking.job;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import com.hxy.module.booking.service.FourAccountReconcileService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 四账对账日任务
 */
@Component
public class FourAccountReconcileDailyJob implements JobHandler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private FourAccountReconcileService reconcileService;

    @Override
    @TenantJob
    public String execute(String param) {
        JobParam jobParam = parseParam(param);
        Long id = reconcileService.runReconcile(jobParam.bizDate, jobParam.source, "SYSTEM");
        return String.format("四账对账完成 id=%s date=%s source=%s", id, jobParam.bizDate, jobParam.source);
    }

    private JobParam parseParam(String param) {
        if (StrUtil.isBlank(param)) {
            return new JobParam(LocalDate.now().minusDays(1), "JOB_DAILY");
        }
        String[] parts = param.trim().split(",");
        LocalDate date = parseDate(parts[0]);
        String source = "JOB_DAILY";
        if (parts.length > 1 && StrUtil.isNotBlank(parts[1])) {
            source = parts[1].trim().toUpperCase();
        }
        return new JobParam(date, source);
    }

    private LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim(), DATE_FORMATTER);
        } catch (Exception ignore) {
            return LocalDate.now().minusDays(1);
        }
    }

    private static class JobParam {
        private final LocalDate bizDate;
        private final String source;

        private JobParam(LocalDate bizDate, String source) {
            this.bizDate = bizDate;
            this.source = source;
        }
    }
}

