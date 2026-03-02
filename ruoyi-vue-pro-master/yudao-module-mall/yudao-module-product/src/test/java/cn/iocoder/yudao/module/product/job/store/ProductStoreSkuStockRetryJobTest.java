package cn.iocoder.yudao.module.product.job.store;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.product.service.store.ProductStoreMappingService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductStoreSkuStockRetryJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ProductStoreSkuStockRetryJob job;

    @Mock
    private ProductStoreMappingService productStoreMappingService;

    @Test
    void execute_shouldUseDefaultLimitWhenParamInvalid() {
        when(productStoreMappingService.retryStoreSkuStockFlow(100)).thenReturn(3);
        String result = job.execute("invalid");
        assertEquals("门店 SKU 库存流水重试完成，成功 3 条", result);
        verify(productStoreMappingService).retryStoreSkuStockFlow(100);
    }

    @Test
    void execute_shouldCapLimit() {
        when(productStoreMappingService.retryStoreSkuStockFlow(1000)).thenReturn(8);
        String result = job.execute("99999");
        assertEquals("门店 SKU 库存流水重试完成，成功 8 条", result);
        verify(productStoreMappingService).retryStoreSkuStockFlow(1000);
    }
}
