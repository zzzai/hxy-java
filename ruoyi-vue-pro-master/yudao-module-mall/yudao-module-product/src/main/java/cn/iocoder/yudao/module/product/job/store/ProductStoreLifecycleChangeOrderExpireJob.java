package cn.iocoder.yudao.module.product.job.store;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 门店生命周期变更单 SLA 超时自动收口任务
 */
@Component
public class ProductStoreLifecycleChangeOrderExpireJob implements JobHandler {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;

    @Resource
    private ProductStoreService productStoreService;

    @Override
    @TenantJob
    public String execute(String param) {
        int limit = parseLimit(param);
        int expired = productStoreService.expirePendingLifecycleChangeOrders(limit);
        return String.format("门店生命周期变更单 SLA 超时收口完成，处理 %s 条", expired);
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
