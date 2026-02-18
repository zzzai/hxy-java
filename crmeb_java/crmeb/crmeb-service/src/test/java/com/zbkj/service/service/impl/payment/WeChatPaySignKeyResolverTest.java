package com.zbkj.service.service.impl.payment;

import com.zbkj.common.constants.Constants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.service.service.SystemConfigService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

class WeChatPaySignKeyResolverTest {

    @Test
    void resolveShouldPreferSpKeyByMchId() {
        Map<String, String> values = new HashMap<>();
        values.put("pay_routine_sp_mchid", "1900001001");
        values.put("pay_routine_sp_key", "routine_sp_key");

        WeChatPaySignKeyResolver resolver = createResolver(values);
        String key = resolver.resolve("wx_any", "1900001001");
        Assertions.assertEquals("routine_sp_key", key);
    }

    @Test
    void resolveShouldUseDirectKeyByDirectMchId() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.CONFIG_KEY_PAY_ROUTINE_MCH_ID, "1900002001");
        values.put(Constants.CONFIG_KEY_PAY_ROUTINE_APP_KEY, "routine_direct_key");
        values.put("pay_routine_sp_mchid", "1900001001");
        values.put("pay_routine_sp_key", "routine_sp_key");

        WeChatPaySignKeyResolver resolver = createResolver(values);
        String key = resolver.resolve("wx_any", "1900002001");
        Assertions.assertEquals("routine_direct_key", key);
    }

    @Test
    void resolveShouldFallbackBySpAppId() {
        Map<String, String> values = new HashMap<>();
        values.put("pay_weixin_sp_appid", "wx_sp_001");
        values.put("pay_weixin_sp_key", "sp_public_key");

        WeChatPaySignKeyResolver resolver = createResolver(values);
        String key = resolver.resolve("wx_sp_001", null);
        Assertions.assertEquals("sp_public_key", key);
    }

    @Test
    void resolveUnknownChannelShouldThrow() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.CONFIG_KEY_PAY_ROUTINE_APP_ID, "wx_routine_001");
        values.put(Constants.CONFIG_KEY_PAY_ROUTINE_APP_KEY, "routine_base_key");

        WeChatPaySignKeyResolver resolver = createResolver(values);
        Assertions.assertThrows(CrmebException.class, () -> resolver.resolve("wx_unknown", "1999999999"));
    }

    private WeChatPaySignKeyResolver createResolver(Map<String, String> values) {
        SystemConfigService systemConfigService = Mockito.mock(SystemConfigService.class);
        Mockito.when(systemConfigService.getValueByKey(Mockito.anyString()))
                .thenAnswer(invocation -> values.getOrDefault(invocation.getArgument(0), ""));
        Mockito.when(systemConfigService.getValueByKeyException(Mockito.anyString()))
                .thenAnswer(invocation -> {
                    String value = values.get(invocation.getArgument(0));
                    if (value == null || value.trim().isEmpty()) {
                        throw new CrmebException("missing config: " + invocation.getArgument(0));
                    }
                    return value;
                });
        WeChatPaySignKeyResolver resolver = new WeChatPaySignKeyResolver();
        ReflectionTestUtils.setField(resolver, "systemConfigService", systemConfigService);
        return resolver;
    }
}
