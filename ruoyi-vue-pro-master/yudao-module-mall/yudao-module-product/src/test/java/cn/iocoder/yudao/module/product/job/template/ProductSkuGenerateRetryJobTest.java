package cn.iocoder.yudao.module.product.job.template;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.product.service.template.ProductTemplateGenerateService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductSkuGenerateRetryJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ProductSkuGenerateRetryJob job;

    @Mock
    private ProductTemplateGenerateService productTemplateGenerateService;

    @Test
    void execute_shouldUseDefaultLimitWhenInvalidParam() {
        when(productTemplateGenerateService.retryFailedCommitTasks(100)).thenReturn(2);
        String result = job.execute("abc");
        assertEquals("SKU 生成失败任务重试完成，处理 2 条", result);
        verify(productTemplateGenerateService).retryFailedCommitTasks(100);
    }

    @Test
    void execute_shouldCapLimit() {
        when(productTemplateGenerateService.retryFailedCommitTasks(1000)).thenReturn(5);
        String result = job.execute("5000");
        assertEquals("SKU 生成失败任务重试完成，处理 5 条", result);
        verify(productTemplateGenerateService).retryFailedCommitTasks(1000);
    }
}
