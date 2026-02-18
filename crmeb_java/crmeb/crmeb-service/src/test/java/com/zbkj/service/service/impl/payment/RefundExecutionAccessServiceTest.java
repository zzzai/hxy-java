package com.zbkj.service.service.impl.payment;

import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.service.service.SystemConfigService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundExecutionAccessServiceTest {

    @Mock
    private SystemConfigService systemConfigService;

    private RefundExecutionAccessService service;

    @BeforeEach
    void setUp() {
        service = new RefundExecutionAccessService();
        ReflectionTestUtils.setField(service, "systemConfigService", systemConfigService);
    }

    @Test
    void shouldAllowAnyAdminWhenHqOnlyDisabled() {
        mockHqOnly("0");
        SystemAdmin admin = admin(9, "12,13");

        Assertions.assertTrue(service.canExecuteManualRefund(admin));
    }

    @Test
    void shouldAllowSuperAdminWhenHqOnlyEnabled() {
        mockHqOnly("1");
        SystemAdmin admin = admin(6, "1,9");

        Assertions.assertTrue(service.canExecuteManualRefund(admin));
    }

    @Test
    void shouldAllowAdminIdWhitelistWhenHqOnlyEnabled() {
        mockHqOnly("1");
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_HQ_ADMIN_IDS)).thenReturn("8,10");
        SystemAdmin admin = admin(10, "7,8");

        Assertions.assertTrue(service.canExecuteManualRefund(admin));
    }

    @Test
    void shouldAllowRoleWhitelistWhenHqOnlyEnabled() {
        mockHqOnly("1");
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_HQ_ADMIN_IDS)).thenReturn("");
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_HQ_ROLE_IDS)).thenReturn("88,99");
        SystemAdmin admin = admin(12, "7,88");

        Assertions.assertTrue(service.canExecuteManualRefund(admin));
    }

    @Test
    void shouldRejectWhenNotInAnyWhitelist() {
        mockHqOnly("1");
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_HQ_ADMIN_IDS)).thenReturn("8,10");
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_HQ_ROLE_IDS)).thenReturn("88,99");
        SystemAdmin admin = admin(12, "7,77");

        Assertions.assertFalse(service.canExecuteManualRefund(admin));
    }

    private void mockHqOnly(String value) {
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_HQ_ONLY_ENABLE)).thenReturn(value);
    }

    private SystemAdmin admin(Integer id, String roles) {
        SystemAdmin admin = new SystemAdmin();
        admin.setId(id);
        admin.setRoles(roles);
        return admin;
    }
}
