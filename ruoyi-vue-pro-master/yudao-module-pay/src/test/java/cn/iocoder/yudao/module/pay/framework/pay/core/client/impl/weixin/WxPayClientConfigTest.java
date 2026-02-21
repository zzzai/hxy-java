package cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.weixin;

import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;

import static org.junit.jupiter.api.Assertions.*;

class WxPayClientConfigTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldEnablePartnerModeWhenSubMchConfigured() {
        WxPayClientConfig config = baseV3Config();
        config.setSubMchId("1900001111");

        assertTrue(config.isPartnerMode());
    }

    @Test
    void shouldRequireSubMchWhenPartnerModeEnabledOnV3() {
        WxPayClientConfig config = baseV3Config();
        config.setPartnerMode(true);
        config.setSubMchId("");

        assertThrows(ConstraintViolationException.class, () -> config.validate(validator));
    }

    @Test
    void shouldPassValidationWhenPartnerModeWithSubMch() {
        WxPayClientConfig config = baseV3Config();
        config.setPartnerMode(true);
        config.setSubMchId("1900001111");

        assertDoesNotThrow(() -> config.validate(validator));
    }

    @Test
    void shouldUseNoContentAckForV3Notify() {
        WxPubPayClient client = new WxPubPayClient(1L, baseV3Config());

        assertEquals(204, client.getNotifySuccessHttpStatus());
        assertNull(client.getNotifySuccessBody());
    }

    @Test
    void shouldUseSuccessBodyAckForV2Notify() {
        WxPayClientConfig config = new WxPayClientConfig();
        config.setApiVersion(WxPayClientConfig.API_VERSION_V2);
        WxPubPayClient client = new WxPubPayClient(1L, config);

        assertEquals(200, client.getNotifySuccessHttpStatus());
        assertEquals("success", client.getNotifySuccessBody());
    }

    private static WxPayClientConfig baseV3Config() {
        WxPayClientConfig config = new WxPayClientConfig();
        config.setAppId("wx1234567890abcdef");
        config.setMchId("1900000109");
        config.setApiVersion(WxPayClientConfig.API_VERSION_V3);
        config.setPrivateKeyContent("-----BEGIN PRIVATE KEY-----\\ntest\\n-----END PRIVATE KEY-----");
        config.setApiV3Key("12345678901234567890123456789012");
        config.setCertSerialNo("57D7B01B06B2A6155BDE3C5A8AEC64AF1643BC5F");
        config.setPublicKeyId("PUB_KEY_ID_DEMO");
        return config;
    }
}
