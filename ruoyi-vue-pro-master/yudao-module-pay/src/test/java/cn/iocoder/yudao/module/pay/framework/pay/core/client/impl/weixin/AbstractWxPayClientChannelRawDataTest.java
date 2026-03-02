package cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.weixin;

import com.github.binarywang.wxpay.bean.WxPayApiData;
import com.github.binarywang.wxpay.exception.WxPayException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbstractWxPayClientChannelRawDataTest {

    @Test
    void buildChannelRawData_preferApiResponseAndAttachUrl() {
        WxPayException exception = new WxPayException("unknown");
        WxPayApiData apiData = new WxPayApiData(
                "https://api.mch.weixin.qq.com/v3/pay/partner/transactions/jsapi",
                "{}",
                "{\"code\":\"SYSTEM_ERROR\",\"message\":\"未知错误\"}",
                null
        );

        String rawData = AbstractWxPayClient.buildChannelRawData(exception, apiData);

        assertNotNull(rawData);
        assertTrue(rawData.startsWith("url=https://api.mch.weixin.qq.com/v3/pay/partner/transactions/jsapi | body="));
        assertTrue(rawData.contains("\"code\":\"SYSTEM_ERROR\""));
    }

    @Test
    void buildChannelRawData_fallbackToExceptionMessageWhenNoApiData() {
        WxPayException exception = new WxPayException("未知错误");

        String rawData = AbstractWxPayClient.buildChannelRawData(exception, null);

        assertEquals("未知错误", rawData);
    }

    @Test
    void buildChannelRawData_truncateToDatabaseSafeLength() {
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < 1400; i++) {
            message.append('x');
        }
        WxPayException exception = new WxPayException(message.toString());

        String rawData = AbstractWxPayClient.buildChannelRawData(exception, null);

        assertNotNull(rawData);
        assertTrue(rawData.length() <= 1000);
        assertTrue(rawData.endsWith("..."));
    }

}
