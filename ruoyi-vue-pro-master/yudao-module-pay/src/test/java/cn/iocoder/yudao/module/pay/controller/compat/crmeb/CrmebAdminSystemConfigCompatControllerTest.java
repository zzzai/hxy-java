package cn.iocoder.yudao.module.pay.controller.compat.crmeb;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.dal.dataobject.app.PayAppDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.channel.PayChannelDO;
import cn.iocoder.yudao.module.pay.dal.mysql.app.PayAppMapper;
import cn.iocoder.yudao.module.pay.dal.mysql.channel.PayChannelMapper;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.weixin.WxPayClientConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrmebAdminSystemConfigCompatControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private CrmebAdminSystemConfigCompatController controller;

    @Mock
    private PayAppMapper payAppMapper;
    @Mock
    private PayChannelMapper payChannelMapper;

    @Test
    void shouldCheckSupportedKey() {
        CrmebCompatResult<Boolean> result = controller.check("pay_routine_api_version");
        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData()));
    }

    @Test
    void shouldGetRoutineApiVersion() {
        PayAppDO app = new PayAppDO();
        app.setId(1L);
        when(payAppMapper.selectByAppKey("mall")).thenReturn(app);

        WxPayClientConfig config = new WxPayClientConfig();
        config.setApiVersion(WxPayClientConfig.API_VERSION_V3);
        PayChannelDO channel = new PayChannelDO();
        channel.setId(100L);
        channel.setAppId(1L);
        channel.setCode("wx_lite");
        channel.setConfig(config);
        when(payChannelMapper.selectByAppIdAndCode(1L, "wx_lite")).thenReturn(channel);

        CrmebCompatResult<CrmebAdminSystemConfigCompatController.CrmebConfigUniqRespVO> result =
                controller.getuniq("pay_routine_api_version", null, null);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("pay_routine_api_version", result.getData().getKey());
        assertEquals("v3", result.getData().getValue());
    }

    @Test
    void shouldMaskSecretWhenGetUniq() {
        PayAppDO app = new PayAppDO();
        app.setId(1L);
        when(payAppMapper.selectByAppKey("mall")).thenReturn(app);

        WxPayClientConfig config = new WxPayClientConfig();
        config.setApiV3Key("real-secret");
        PayChannelDO channel = new PayChannelDO();
        channel.setId(101L);
        channel.setAppId(1L);
        channel.setCode("wx_lite");
        channel.setConfig(config);
        when(payChannelMapper.selectByAppIdAndCode(1L, "wx_lite")).thenReturn(channel);

        CrmebCompatResult<CrmebAdminSystemConfigCompatController.CrmebConfigUniqRespVO> result =
                controller.getuniq("pay_routine_apiv3_key", null, null);

        assertEquals(200, result.getCode());
        assertEquals("******", result.getData().getValue());
    }

    @Test
    void shouldSaveRoutineAppIdToChannelConfig() {
        PayAppDO app = new PayAppDO();
        app.setId(1L);
        when(payAppMapper.selectByAppKey("mall")).thenReturn(app);

        WxPayClientConfig oldConfig = new WxPayClientConfig();
        oldConfig.setAppId("old-app-id");
        oldConfig.setApiVersion("v3");
        PayChannelDO channel = new PayChannelDO();
        channel.setId(102L);
        channel.setAppId(1L);
        channel.setCode("wx_lite");
        channel.setConfig(oldConfig);
        when(payChannelMapper.selectByAppIdAndCode(1L, "wx_lite")).thenReturn(channel);
        when(payChannelMapper.updateById(any(PayChannelDO.class))).thenReturn(1);

        CrmebCompatResult<Boolean> result = controller.saveuniq("pay_routine_appid", "wx_new_app", null, null);

        assertEquals(200, result.getCode());
        ArgumentCaptor<PayChannelDO> captor = ArgumentCaptor.forClass(PayChannelDO.class);
        verify(payChannelMapper).updateById(captor.capture());
        assertEquals(102L, captor.getValue().getId());
        WxPayClientConfig updatedConfig = (WxPayClientConfig) captor.getValue().getConfig();
        assertEquals("wx_new_app", updatedConfig.getAppId());
    }

    @Test
    void shouldToggleWechatSwitchByUpdatingAllWechatChannels() {
        PayChannelDO wxLite = new PayChannelDO();
        wxLite.setId(201L);
        wxLite.setCode("wx_lite");
        wxLite.setStatus(CommonStatusEnum.DISABLE.getStatus());
        PayChannelDO wxPub = new PayChannelDO();
        wxPub.setId(202L);
        wxPub.setCode("wx_pub");
        wxPub.setStatus(CommonStatusEnum.DISABLE.getStatus());
        PayChannelDO wallet = new PayChannelDO();
        wallet.setId(203L);
        wallet.setCode("wallet");
        wallet.setStatus(CommonStatusEnum.DISABLE.getStatus());
        when(payChannelMapper.selectList()).thenReturn(Arrays.asList(wxLite, wxPub, wallet));

        CrmebCompatResult<Boolean> result = controller.saveuniq("pay_weixin_open", "1", null, null);

        assertEquals(200, result.getCode());
        ArgumentCaptor<PayChannelDO> captor = ArgumentCaptor.forClass(PayChannelDO.class);
        verify(payChannelMapper, times(2)).updateById(captor.capture());
        assertEquals(2, captor.getAllValues().size());
        assertTrue(captor.getAllValues().stream()
                .allMatch(item -> CommonStatusEnum.ENABLE.getStatus().equals(item.getStatus())));
    }

    @Test
    void shouldFailWhenKeyUnsupported() {
        CrmebCompatResult<Boolean> result = controller.saveuniq("unknown_key", "1", null, null);
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("暂不支持"));
    }

    @Test
    void shouldCheckAliasKeyAsSupported() {
        CrmebCompatResult<Boolean> result = controller.check("pay_routine_app_key");
        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData()));
    }

    @Test
    void shouldSaveAliasMiniClientP12AsBase64() throws Exception {
        PayAppDO app = new PayAppDO();
        app.setId(1L);
        when(payAppMapper.selectByAppKey("mall")).thenReturn(app);

        WxPayClientConfig oldConfig = new WxPayClientConfig();
        oldConfig.setApiVersion("v2");
        PayChannelDO channel = new PayChannelDO();
        channel.setId(301L);
        channel.setAppId(1L);
        channel.setCode("wx_lite");
        channel.setConfig(oldConfig);
        when(payChannelMapper.selectByAppIdAndCode(1L, "wx_lite")).thenReturn(channel);
        when(payChannelMapper.updateById(any(PayChannelDO.class))).thenReturn(1);

        Path p12 = Files.createTempFile("crmeb-mini-", ".p12");
        Files.write(p12, "p12-content".getBytes(StandardCharsets.UTF_8));
        try {
            CrmebCompatResult<Boolean> result = controller.saveuniq("pay_mini_client_p12", p12.toString(), null, null);
            assertEquals(200, result.getCode());

            ArgumentCaptor<PayChannelDO> captor = ArgumentCaptor.forClass(PayChannelDO.class);
            verify(payChannelMapper).updateById(captor.capture());
            WxPayClientConfig updated = (WxPayClientConfig) captor.getValue().getConfig();
            assertEquals(Base64.getEncoder().encodeToString("p12-content".getBytes(StandardCharsets.UTF_8)),
                    updated.getKeyContent());
        } finally {
            Files.deleteIfExists(p12);
        }
    }

    @Test
    void shouldSaveQuotedWechatSwitchValue() {
        PayChannelDO wxLite = new PayChannelDO();
        wxLite.setId(401L);
        wxLite.setCode("wx_lite");
        wxLite.setStatus(CommonStatusEnum.DISABLE.getStatus());
        when(payChannelMapper.selectList()).thenReturn(Arrays.asList(wxLite));

        CrmebCompatResult<Boolean> result = controller.saveuniq("pay_weixin_open", "'1'", null, null);

        assertEquals(200, result.getCode());
        ArgumentCaptor<PayChannelDO> captor = ArgumentCaptor.forClass(PayChannelDO.class);
        verify(payChannelMapper).updateById(captor.capture());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), captor.getValue().getStatus());
    }

}
