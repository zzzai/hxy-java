package com.zbkj.service.service.impl.payment;

import com.zbkj.service.service.SystemConfigService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentConfigStartupCheckerTest {

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private WeChatPayConfigSupport weChatPayConfigSupport;

    private PaymentConfigStartupChecker checker;

    @BeforeEach
    void setUp() {
        checker = new PaymentConfigStartupChecker();
        ReflectionTestUtils.setField(checker, "checkEnabled", true);
        ReflectionTestUtils.setField(checker, "systemConfigService", systemConfigService);
        ReflectionTestUtils.setField(checker, "weChatPayConfigSupport", weChatPayConfigSupport);
    }

    @Test
    void shouldCheckRoutineWhenProviderV3Configured() {
        Map<String, String> values = new HashMap<>();
        values.put("pay_routine_sp_mchid", "1900000100");
        values.put("pay_routine_sp_apiv3_key", "v3_key");
        values.put("pay_routine_sp_serial_no", "serial");
        values.put("pay_routine_sp_private_key_path", "/tmp/sp_key.pem");
        values.put("pay_routine_sp_platform_cert_path", "/tmp/platform.pem");
        mockConfig(values);

        Boolean result = ReflectionTestUtils.invokeMethod(checker, "shouldCheckRoutine");
        Assertions.assertTrue(Boolean.TRUE.equals(result));
    }

    @Test
    void shouldCheckRoutineWhenDirectV3Configured() {
        Map<String, String> values = new HashMap<>();
        values.put("pay_routine_appid", "wx123");
        values.put("pay_routine_mchid", "1900000100");
        values.put("pay_routine_apiv3_key", "v3_key");
        values.put("pay_routine_serial_no", "serial");
        values.put("pay_routine_private_key_path", "/tmp/key.pem");
        values.put("pay_routine_platform_cert_path", "/tmp/platform.pem");
        mockConfig(values);

        Boolean result = ReflectionTestUtils.invokeMethod(checker, "shouldCheckRoutine");
        Assertions.assertTrue(Boolean.TRUE.equals(result));
    }

    @Test
    void runShouldTriggerRoutineValidationUnderProviderV3Config() {
        Map<String, String> values = new HashMap<>();
        values.put("pay_weixin_open", "1");
        values.put("pay_routine_sp_mchid", "1900000100");
        values.put("pay_routine_sp_apiv3_key", "v3_key");
        values.put("pay_routine_sp_serial_no", "serial");
        values.put("pay_routine_sp_private_key_path", "/tmp/sp_key.pem");
        values.put("pay_routine_sp_platform_cert_path", "/tmp/platform.pem");
        values.put("pay_routine_sub_mchid", "1900000200");
        mockConfig(values);

        checker.run();

        verify(weChatPayConfigSupport).resolveByChannel(1, 0);
    }

    private void mockConfig(Map<String, String> values) {
        when(systemConfigService.getValueByKey(anyString()))
                .thenAnswer(invocation -> values.getOrDefault(invocation.getArgument(0), ""));
    }
}
