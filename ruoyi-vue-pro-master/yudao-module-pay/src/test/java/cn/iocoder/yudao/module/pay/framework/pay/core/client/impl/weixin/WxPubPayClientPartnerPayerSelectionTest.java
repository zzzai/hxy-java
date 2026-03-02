package cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.weixin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.dto.order.PayOrderUnifiedReqDTO;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WxPubPayClientPartnerPayerSelectionTest {

    @Test
    void selectPartnerPayer_useSubByDefaultWhenSubAppIdPresent() {
        PayOrderUnifiedReqDTO reqDTO = buildReq(Map.of("openid", "openid-default"));

        WxPubPayClient.PartnerPayerSelection selection = WxPubPayClient.selectPartnerPayer(reqDTO, "wx-sub-app-id");

        assertTrue(selection.isUseSubAppId());
        assertEquals("openid-default", selection.getPayer().getSubOpenid());
        assertNull(selection.getPayer().getSpOpenid());
    }

    @Test
    void selectPartnerPayer_useSpWhenExplicitMode() {
        Map<String, String> extras = new HashMap<>();
        extras.put("openid", "openid-default");
        extras.put("spOpenid", "openid-sp");
        extras.put("partnerPayerMode", "sp");
        PayOrderUnifiedReqDTO reqDTO = buildReq(extras);

        WxPubPayClient.PartnerPayerSelection selection = WxPubPayClient.selectPartnerPayer(reqDTO, "");

        assertFalse(selection.isUseSubAppId());
        assertEquals("openid-sp", selection.getPayer().getSpOpenid());
        assertNull(selection.getPayer().getSubOpenid());
    }

    @Test
    void selectPartnerPayer_throwWhenForceSpButSubAppIdPresent() {
        PayOrderUnifiedReqDTO reqDTO = buildReq(Map.of(
                "openid", "openid-default",
                "partnerPayerMode", "sp"
        ));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> WxPubPayClient.selectPartnerPayer(reqDTO, "wx-sub-app-id"));
        assertEquals("支付请求的 partnerPayerMode=sp 时，支付渠道 subAppId 必须置空！", exception.getMessage());
    }

    @Test
    void selectPartnerPayer_useSpWhenSubAppIdMissing() {
        PayOrderUnifiedReqDTO reqDTO = buildReq(Map.of("openid", "openid-default"));

        WxPubPayClient.PartnerPayerSelection selection = WxPubPayClient.selectPartnerPayer(reqDTO, null);

        assertFalse(selection.isUseSubAppId());
        assertEquals("openid-default", selection.getPayer().getSpOpenid());
        assertNull(selection.getPayer().getSubOpenid());
    }

    @Test
    void selectPartnerPayer_throwWhenForceSubButSubAppIdMissing() {
        PayOrderUnifiedReqDTO reqDTO = buildReq(Map.of(
                "openid", "openid-default",
                "partnerPayerMode", "sub"
        ));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> WxPubPayClient.selectPartnerPayer(reqDTO, ""));
        assertEquals("支付请求的 partnerPayerMode=sub 时，subAppId 不能为空！", exception.getMessage());
    }

    private static PayOrderUnifiedReqDTO buildReq(Map<String, String> channelExtras) {
        PayOrderUnifiedReqDTO reqDTO = new PayOrderUnifiedReqDTO();
        reqDTO.setChannelExtras(new HashMap<>(channelExtras));
        return reqDTO;
    }

}
