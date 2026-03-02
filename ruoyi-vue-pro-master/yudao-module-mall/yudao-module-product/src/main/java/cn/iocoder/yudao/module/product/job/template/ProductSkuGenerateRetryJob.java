package cn.iocoder.yudao.module.product.job.template;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.product.service.template.ProductTemplateGenerateService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * SKU 生成失败任务重试 Job
 */
@Component
public class ProductSkuGenerateRetryJob implements JobHandler {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;

    @Resource
    private ProductTemplateGenerateService productTemplateGenerateService;

    @Override
    @TenantJob
    public String execute(String param) {
        int limit = parseLimit(param);
        int retryCount = productTemplateGenerateService.retryFailedCommitTasks(limit);
        return String.format("SKU 生成失败任务重试完成，处理 %s 条", retryCount);
    }

    private static int parseLimit(String param) {
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
