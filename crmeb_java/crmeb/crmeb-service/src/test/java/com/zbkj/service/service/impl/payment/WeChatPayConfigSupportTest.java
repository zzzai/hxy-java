package com.zbkj.service.service.impl.payment;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.vo.WeChatPayChannelConfig;
import com.zbkj.service.service.SystemConfigService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

class WeChatPayConfigSupportTest {

    @Test
    void resolveRoutineProviderModeWithoutLegacyBaseConfig() {
        Map<String, String> values = new HashMap<>();
        values.put("pay_routine_sp_appid", "wx_sp_100");
        values.put("pay_routine_sp_mchid", "1900000100");
        values.put("pay_routine_sp_key", "sp_key_001");
        values.put("pay_routine_sub_mchid", "1900000200");

        WeChatPayConfigSupport support = createSupport(values);
        WeChatPayChannelConfig config = support.resolveByChannel(1, 0);

        Assertions.assertTrue(config.getServiceProviderMode());
        Assertions.assertEquals("wx_sp_100", config.getAppId());
        Assertions.assertEquals("wx_sp_100", config.getClientAppId());
        Assertions.assertEquals("1900000100", config.getMchId());
        Assertions.assertEquals("1900000200", config.getSubMchId());
        Assertions.assertEquals("sp_key_001", config.getSignKey());
    }

    @Test
    void resolveRoutineProviderModeUseStoreSpecificSubMerchant() {
        Map<String, String> values = new HashMap<>();
        values.put("pay_routine_appid", "wx_base_001");
        values.put("pay_routine_sp_appid", "wx_sp_001");
        values.put("pay_routine_sp_mchid", "1900000001");
        values.put("pay_routine_sp_key", "sp_key_001");
        values.put("pay_routine_sub_mchid", "1900000099");
        values.put("pay_routine_sub_mchid_12", "1900000012");
        values.put("pay_routine_sub_appid_12", "wx_sub_12");

        WeChatPayConfigSupport support = createSupport(values);
        WeChatPayChannelConfig config = support.resolveByChannel(1, 12);

        Assertions.assertEquals("1900000012", config.getSubMchId());
        Assertions.assertEquals("wx_sub_12", config.getSubAppId());
        Assertions.assertEquals("wx_sub_12", config.getClientAppId());
        Assertions.assertEquals("wx_sp_001", config.getAppId());
    }

    @Test
    void resolveDirectModeMissingRequiredFieldShouldFailFast() {
        Map<String, String> values = new HashMap<>();
        values.put("pay_routine_appid", "wx_base_001");
        values.put("pay_routine_mchid", "1901000001");
        // pay_routine_key 缺失

        WeChatPayConfigSupport support = createSupport(values);
        CrmebException ex = Assertions.assertThrows(CrmebException.class, () -> support.resolveByChannel(1, 0));
        Assertions.assertTrue(ex.getMessage().contains("pay_routine_key"));
    }

    @Test
    void resolveProviderModeMissingSubMerchantShouldFailFast() {
        Map<String, String> values = new HashMap<>();
        values.put("pay_routine_sp_appid", "wx_sp_100");
        values.put("pay_routine_sp_mchid", "1900000100");
        values.put("pay_routine_sp_key", "sp_key_001");
        // 未配置 pay_routine_sub_mchid

        WeChatPayConfigSupport support = createSupport(values);
        CrmebException ex = Assertions.assertThrows(CrmebException.class, () -> support.resolveByChannel(1, 0));
        Assertions.assertTrue(ex.getMessage().contains("子商户号"));
    }

    private WeChatPayConfigSupport createSupport(Map<String, String> values) {
        SystemConfigService systemConfigService = Mockito.mock(SystemConfigService.class);
        Mockito.when(systemConfigService.getValueByKey(Mockito.anyString()))
                .thenAnswer(invocation -> values.getOrDefault(invocation.getArgument(0), ""));
        WeChatPayConfigSupport support = new WeChatPayConfigSupport();
        ReflectionTestUtils.setField(support, "systemConfigService", systemConfigService);
        return support;
    }
}
