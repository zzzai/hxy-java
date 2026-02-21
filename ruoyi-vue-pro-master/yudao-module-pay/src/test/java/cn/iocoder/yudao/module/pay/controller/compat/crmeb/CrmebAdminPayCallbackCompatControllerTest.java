package cn.iocoder.yudao.module.pay.controller.compat.crmeb;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.controller.admin.notify.PayNotifyController;
import cn.iocoder.yudao.module.pay.dal.dataobject.channel.PayChannelDO;
import cn.iocoder.yudao.module.pay.dal.mysql.channel.PayChannelMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrmebAdminPayCallbackCompatControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private CrmebAdminPayCallbackCompatController controller;

    @Mock
    private PayNotifyController payNotifyController;
    @Mock
    private PayChannelMapper payChannelMapper;

    @Test
    void shouldUseInputChannelIdWhenProvided() {
        when(payNotifyController.notifyOrder(eq(9L), anyMap(), anyString(), anyMap()))
                .thenReturn(ResponseEntity.noContent().build());

        Map<String, String> params = Collections.singletonMap("id", "event_1");
        Map<String, String> headers = Collections.singletonMap("wechatpay-signature", "sign_1");
        ResponseEntity<String> response = controller.wechat(9L, params, "{\"id\":\"event_1\"}", headers);

        assertEquals(204, response.getStatusCodeValue());
        verify(payNotifyController).notifyOrder(eq(9L), eq(params), eq("{\"id\":\"event_1\"}"), eq(headers));
    }

    @Test
    void shouldUseDefaultChannelIdFromConfig() {
        ReflectionTestUtils.setField(controller, "defaultWechatChannelId", "12");
        when(payNotifyController.notifyRefund(eq(12L), anyMap(), anyString(), anyMap()))
                .thenReturn(ResponseEntity.ok("success"));

        Map<String, String> params = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        ResponseEntity<String> response = controller.wechatRefund(null, params, "{\"id\":\"event_refund\"}", headers);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("success", response.getBody());
        verify(payNotifyController).notifyRefund(eq(12L), eq(params), eq("{\"id\":\"event_refund\"}"), eq(headers));
    }

    @Test
    void shouldAutoResolveSingleEnabledWechatChannel() {
        ReflectionTestUtils.setField(controller, "defaultWechatChannelId", "");
        PayChannelDO channel = new PayChannelDO();
        channel.setId(7L);
        when(payChannelMapper.selectListByCodePrefixAndStatus(eq("wx_"), eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Collections.singletonList(channel));
        when(payNotifyController.notifyOrder(eq(7L), anyMap(), anyString(), anyMap()))
                .thenReturn(ResponseEntity.noContent().build());

        ResponseEntity<String> response = controller.wechat(null, Collections.emptyMap(), "{}", Collections.emptyMap());

        assertEquals(204, response.getStatusCodeValue());
        verify(payNotifyController).notifyOrder(eq(7L), eq(Collections.emptyMap()), eq("{}"), eq(Collections.emptyMap()));
    }

    @Test
    void shouldThrowWhenMultipleEnabledWechatChannels() {
        ReflectionTestUtils.setField(controller, "defaultWechatChannelId", "");
        PayChannelDO channel1 = new PayChannelDO();
        channel1.setId(1L);
        PayChannelDO channel2 = new PayChannelDO();
        channel2.setId(2L);
        when(payChannelMapper.selectListByCodePrefixAndStatus(eq("wx_"), eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(java.util.Arrays.asList(channel1, channel2));

        assertThrows(ServiceException.class,
                () -> controller.wechat(null, Collections.emptyMap(), "{}", Collections.emptyMap()));
    }

    @Test
    void shouldThrowWhenNoEnabledWechatChannels() {
        ReflectionTestUtils.setField(controller, "defaultWechatChannelId", "");
        when(payChannelMapper.selectListByCodePrefixAndStatus(eq("wx_"), eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Collections.emptyList());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> controller.wechat(null, Collections.emptyMap(), "{}", Collections.emptyMap()));
        assertTrue(exception.getMessage().contains("未找到可用微信支付渠道"));
    }

    @Test
    void shouldFallbackToAutoResolveWhenDefaultChannelIdInvalid() {
        ReflectionTestUtils.setField(controller, "defaultWechatChannelId", "invalid_channel_id");
        PayChannelDO channel = new PayChannelDO();
        channel.setId(19L);
        when(payChannelMapper.selectListByCodePrefixAndStatus(eq("wx_"), eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Collections.singletonList(channel));
        when(payNotifyController.notifyRefund(eq(19L), anyMap(), anyString(), anyMap()))
                .thenReturn(ResponseEntity.ok("success"));

        ResponseEntity<String> response = controller.wechatRefund(null, Collections.emptyMap(), "{}", Collections.emptyMap());

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("success", response.getBody());
        verify(payNotifyController).notifyRefund(eq(19L), eq(Collections.emptyMap()), eq("{}"), eq(Collections.emptyMap()));
    }
}
