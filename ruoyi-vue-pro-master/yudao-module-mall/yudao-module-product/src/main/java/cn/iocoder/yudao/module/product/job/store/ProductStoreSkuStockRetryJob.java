package cn.iocoder.yudao.module.product.job.store;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.product.service.store.ProductStoreMappingService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 门店 SKU 库存流水重试任务
 */
@Component
public class ProductStoreSkuStockRetryJob implements JobHandler {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;

    @Resource
    private ProductStoreMappingService productStoreMappingService;

    @Override
    @TenantJob
    public String execute(String param) {
        int limit = parseLimit(param);
        int successCount = productStoreMappingService.retryStoreSkuStockFlow(limit);
        return String.format("门店 SKU 库存流水重试完成，成功 %s 条", successCount);
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
