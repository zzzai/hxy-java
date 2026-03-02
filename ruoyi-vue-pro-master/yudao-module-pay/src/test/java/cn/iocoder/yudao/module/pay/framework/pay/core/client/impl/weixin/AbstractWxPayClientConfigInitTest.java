package cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.weixin;

import com.github.binarywang.wxpay.config.WxPayConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractWxPayClientConfigInitTest {

    @Test
    void shouldEnableFullPublicKeyModeWhenPublicKeyConfigured() {
        WxPayClientConfig config = buildBaseV3Config();
        config.setPublicKeyId("PUB_KEY_ID_TEST");
        config.setPublicKeyContent("-----BEGIN PUBLIC KEY-----\n"
                + "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBALe3mW8Ues4XqkH2dOOnZDG4V3r7Q2r2\n"
                + "m9m9L2kQzYkQrk5v4Qv9eQYB4x4Q4YjK9J0A9Hh6Qj8F3xj7l2O9V8ECAwEAAQ==\n"
                + "-----END PUBLIC KEY-----");

        TestWxPubPayClient client = new TestWxPubPayClient(1L, config);
        client.init();

        WxPayConfig wxPayConfig = client.getWxPayConfig();
        assertTrue(wxPayConfig.isFullPublicKeyModel(),
                "public key已配置时，应启用fullPublicKeyModel，避免走平台证书自动拉取");
    }

    @Test
    void shouldKeepNonPublicKeyModeWhenPublicKeyMissing() {
        WxPayClientConfig config = buildBaseV3Config();
        config.setPublicKeyId("PUB_KEY_ID_TEST");

        TestWxPubPayClient client = new TestWxPubPayClient(1L, config);
        client.init();

        WxPayConfig wxPayConfig = client.getWxPayConfig();
        assertFalse(wxPayConfig.isFullPublicKeyModel(),
                "未配置publicKeyContent时，不应启用fullPublicKeyModel");
    }

    private static WxPayClientConfig buildBaseV3Config() {
        WxPayClientConfig config = new WxPayClientConfig();
        config.setApiVersion(WxPayClientConfig.API_VERSION_V3);
        config.setPartnerMode(true);
        config.setAppId("wx97fb30aed3983c2c");
        config.setMchId("1739427215");
        config.setSubMchId("1106655249");
        config.setSubAppId("wx97fb30aed3983c2c");
        config.setApiV3Key("0123456789ABCDEF0123456789ABCDEF");
        config.setCertSerialNo("2453A4F2A3DCCDCFC314C35935ACAC03A03F8B06");
        config.setPrivateKeyContent("-----BEGIN PRIVATE KEY-----\n"
                + "MIIBVwIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAt7eZbxR6zheqQfZ0\n"
                + "46dkMbhXevtDavab2b0vaRDNiRCuTm/hC/15BgHjHhDhiMr0nQD0eHpCPwXfGPuX\n"
                + "Y71XwQIDAQABAkEAqkW5udP8M+e8rPnWmrQmB4p2sI7M1Z8V0j0wR9x5tYc3bH4M\n"
                + "Kf8m2R1+H6uR3x7o2uYH9W8d7b4q3N2m1kzQIQIhAPrP8z4J4h3W2m+S8Yz4v3b2\n"
                + "8QW5jFvM8J7q3B9h0+1LAiEAu8J6r3G5m8t2k9Q4f1p6s3d8h5j2k7m4n1p8s5q2\n"
                + "c0MCIQCSf4Q8m2n7b3k9t5p1w6r2d8h4j0m6n2p8s4q0b6c3sQIhAKm5p9r3t7w1\n"
                + "y5a9d3h7l1p5t9x3b7f1j5n9r3v7z1qNAiEAq9m5t1x7b3f9j5n1r7v3z9d5h1l7\n"
                + "p3t9x5b1f7j3n9r5v1w=\n"
                + "-----END PRIVATE KEY-----");
        return config;
    }

    private static final class TestWxPubPayClient extends WxPubPayClient {

        private TestWxPubPayClient(Long channelId, WxPayClientConfig config) {
            super(channelId, "wx_lite", config);
        }

        private WxPayConfig getWxPayConfig() {
            return client.getConfig();
        }
    }

}
