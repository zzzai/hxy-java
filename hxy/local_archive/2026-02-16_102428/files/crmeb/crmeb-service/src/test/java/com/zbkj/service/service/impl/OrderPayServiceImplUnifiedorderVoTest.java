package com.zbkj.service.service.impl;

import com.zbkj.common.constants.Constants;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.vo.CreateOrderRequestVo;
import com.zbkj.common.vo.WeChatPayChannelConfig;
import com.zbkj.service.service.SystemConfigService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

class OrderPayServiceImplUnifiedorderVoTest {

    @Test
    void providerModeWithDifferentSubAppIdShouldUseSubOpenId() {
        OrderPayServiceImpl service = createService();
        StoreOrder order = createStoreOrder();
        WeChatPayChannelConfig config = new WeChatPayChannelConfig()
                .setServiceProviderMode(true)
                .setAppId("wx_sp_001")
                .setSubAppId("wx_sub_001")
                .setSubMchId("1900002222")
                .setMchId("1900001111")
                .setSignKey("test_sign_key");

        CreateOrderRequestVo vo = ReflectionTestUtils.invokeMethod(
                service, "getUnifiedorderVo", order, "openid_abc", "127.0.0.1", config
        );

        Assertions.assertNotNull(vo);
        Assertions.assertEquals("openid_abc", vo.getSub_openid());
        Assertions.assertNull(vo.getOpenid());
    }

    @Test
    void providerModeWithSameAppIdShouldUseOpenId() {
        OrderPayServiceImpl service = createService();
        StoreOrder order = createStoreOrder();
        WeChatPayChannelConfig config = new WeChatPayChannelConfig()
                .setServiceProviderMode(true)
                .setAppId("wx_sp_001")
                .setSubAppId("wx_sp_001")
                .setSubMchId("1900002222")
                .setMchId("1900001111")
                .setSignKey("test_sign_key");

        CreateOrderRequestVo vo = ReflectionTestUtils.invokeMethod(
                service, "getUnifiedorderVo", order, "openid_abc", "127.0.0.1", config
        );

        Assertions.assertNotNull(vo);
        Assertions.assertEquals("openid_abc", vo.getOpenid());
        Assertions.assertNull(vo.getSub_openid());
    }

    @Test
    void directModeShouldUseOpenIdOnly() {
        OrderPayServiceImpl service = createService();
        StoreOrder order = createStoreOrder();
        WeChatPayChannelConfig config = new WeChatPayChannelConfig()
                .setServiceProviderMode(false)
                .setAppId("wx_direct_001")
                .setMchId("1900009999")
                .setSignKey("test_sign_key");

        CreateOrderRequestVo vo = ReflectionTestUtils.invokeMethod(
                service, "getUnifiedorderVo", order, "openid_abc", "127.0.0.1", config
        );

        Assertions.assertNotNull(vo);
        Assertions.assertEquals("openid_abc", vo.getOpenid());
        Assertions.assertNull(vo.getSub_openid());
    }

    private OrderPayServiceImpl createService() {
        OrderPayServiceImpl service = new OrderPayServiceImpl();
        SystemConfigService systemConfigService = Mockito.mock(SystemConfigService.class);

        Map<String, String> values = new HashMap<>();
        values.put(Constants.CONFIG_KEY_SITE_URL, "https://example.com");
        values.put(Constants.CONFIG_KEY_API_URL, "https://api.example.com");
        values.put(Constants.CONFIG_KEY_SITE_NAME, "荷小悦");

        Mockito.when(systemConfigService.getValueByKeyException(Mockito.anyString()))
                .thenAnswer(invocation -> values.getOrDefault(invocation.getArgument(0), "default"));

        ReflectionTestUtils.setField(service, "systemConfigService", systemConfigService);
        return service;
    }

    private StoreOrder createStoreOrder() {
        StoreOrder order = new StoreOrder();
        order.setUid(10001);
        order.setIsChannel(1);
        order.setPayPrice(new BigDecimal("9.90"));
        return order;
    }
}
