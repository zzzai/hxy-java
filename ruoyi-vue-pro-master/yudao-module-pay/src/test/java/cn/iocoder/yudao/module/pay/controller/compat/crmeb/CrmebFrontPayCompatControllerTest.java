package cn.iocoder.yudao.module.pay.controller.compat.crmeb;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.api.wallet.PayWalletApi;
import cn.iocoder.yudao.module.pay.api.wallet.dto.PayWalletRespDTO;
import cn.iocoder.yudao.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import cn.iocoder.yudao.module.pay.controller.admin.order.vo.PayOrderSubmitRespVO;
import cn.iocoder.yudao.module.pay.dal.dataobject.channel.PayChannelDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.order.PayOrderDO;
import cn.iocoder.yudao.module.pay.dal.mysql.channel.PayChannelMapper;
import cn.iocoder.yudao.module.pay.dal.mysql.order.PayOrderMapper;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.pay.framework.pay.core.enums.PayOrderDisplayModeEnum;
import cn.iocoder.yudao.module.pay.service.order.PayOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrmebFrontPayCompatControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private CrmebFrontPayCompatController controller;

    @Mock
    private PayOrderService payOrderService;
    @Mock
    private PayOrderMapper payOrderMapper;
    @Mock
    private PayChannelMapper payChannelMapper;
    @Mock
    private PayWalletApi payWalletApi;

    @Test
    void shouldReturnPayConfig() {
        mockLoginUser(100L);
        when(payChannelMapper.selectListByCodePrefixAndStatus("wx_", CommonStatusEnum.ENABLE.getStatus()))
                .thenReturn(List.of(new PayChannelDO()));
        when(payChannelMapper.selectListByCodePrefixAndStatus("wallet", CommonStatusEnum.ENABLE.getStatus()))
                .thenReturn(List.of(new PayChannelDO()));
        PayWalletRespDTO wallet = new PayWalletRespDTO();
        wallet.setBalance(1234);
        when(payWalletApi.getOrCreateWallet(100L, UserTypeEnum.MEMBER.getValue())).thenReturn(wallet);

        CrmebCompatResult<CrmebFrontPayCompatController.CrmebPayConfigRespVO> result = controller.getPayConfig();

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(Boolean.TRUE.equals(result.getData().getPayWechatOpen()));
        assertTrue(Boolean.TRUE.equals(result.getData().getYuePayStatus()));
        assertEquals("12.34", result.getData().getUserBalance().toPlainString());
    }

    @Test
    void shouldRejectGetPayConfigWhenNotLogin() {
        SecurityContextHolder.clearContext();

        CrmebCompatResult<CrmebFrontPayCompatController.CrmebPayConfigRespVO> result = controller.getPayConfig();

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("请先登录"));
    }

    @Test
    void shouldSubmitPaymentWithMappedChannelAndOpenidFallback() {
        CrmebFrontPayCompatController.CrmebOrderPayRequest reqVO = new CrmebFrontPayCompatController.CrmebOrderPayRequest();
        reqVO.setOrderNo("order_001");
        reqVO.setPayType("weixin");
        reqVO.setPayChannel("routine");
        reqVO.setScene(1177);

        PayOrderDO order = new PayOrderDO();
        order.setId(1L);
        order.setChannelUserId("openid_from_order");
        when(payOrderService.getOrder("order_001")).thenReturn(order);

        PayOrderSubmitRespVO submitRespVO = new PayOrderSubmitRespVO();
        submitRespVO.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        submitRespVO.setDisplayMode(PayOrderDisplayModeEnum.APP.getMode());
        submitRespVO.setDisplayContent("{\"packageValue\":\"prepay_id=wx123\",\"timeStamp\":\"1\"}");
        when(payOrderService.submitOrder(any(PayOrderSubmitReqVO.class), anyString())).thenReturn(submitRespVO);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        CrmebCompatResult<CrmebFrontPayCompatController.CrmebOrderPayResultRespVO> result = controller.payment(reqVO, request);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("order_001", result.getData().getOrderNo());
        assertEquals("weixin", result.getData().getPayType());
        assertNotNull(result.getData().getJsConfig());
        assertEquals("prepay_id=wx123", result.getData().getJsConfig().get("package"));
        assertEquals("prepay_id=wx123", result.getData().getJsConfig().get("packages"));

        ArgumentCaptor<PayOrderSubmitReqVO> captor = ArgumentCaptor.forClass(PayOrderSubmitReqVO.class);
        verify(payOrderService).submitOrder(captor.capture(), anyString());
        assertEquals(1L, captor.getValue().getId());
        assertEquals("wx_lite", captor.getValue().getChannelCode());
        assertNotNull(captor.getValue().getChannelExtras());
        assertEquals("openid_from_order", captor.getValue().getChannelExtras().get("openid"));
        assertEquals("1177", captor.getValue().getChannelExtras().get("scene"));
    }

    @Test
    void shouldSyncAndReturnTrueWhenOrderWaiting() {
        PayOrderDO waitingOrder = new PayOrderDO();
        waitingOrder.setId(2L);
        waitingOrder.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        when(payOrderService.getOrder("order_002")).thenReturn(waitingOrder);

        PayOrderDO latestOrder = new PayOrderDO();
        latestOrder.setId(2L);
        latestOrder.setStatus(PayOrderStatusEnum.SUCCESS.getStatus());
        when(payOrderService.getOrder(2L)).thenReturn(latestOrder);

        CrmebCompatResult<Boolean> result = controller.queryPayResult("order_002");

        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(payOrderService).syncOrderQuietly(2L);
    }

    @Test
    void shouldReturnFailedWhenPayChannelUnsupported() {
        CrmebFrontPayCompatController.CrmebOrderPayRequest reqVO = new CrmebFrontPayCompatController.CrmebOrderPayRequest();
        reqVO.setOrderNo("order_003");
        reqVO.setPayType("weixin");
        reqVO.setPayChannel("unknown");

        PayOrderDO order = new PayOrderDO();
        order.setId(3L);
        when(payOrderService.getOrder("order_003")).thenReturn(order);

        CrmebCompatResult<CrmebFrontPayCompatController.CrmebOrderPayResultRespVO> result = controller.payment(reqVO, new MockHttpServletRequest());

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("暂不支持"));
    }

    @Test
    void shouldSubmitPaymentWithH5AliasChannel() {
        CrmebFrontPayCompatController.CrmebOrderPayRequest reqVO = new CrmebFrontPayCompatController.CrmebOrderPayRequest();
        reqVO.setOrderNo("order_004");
        reqVO.setPayType("weixin");
        reqVO.setPayChannel("h5");

        PayOrderDO order = new PayOrderDO();
        order.setId(4L);
        when(payOrderService.getOrder("order_004")).thenReturn(order);

        PayOrderSubmitRespVO submitRespVO = new PayOrderSubmitRespVO();
        submitRespVO.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        submitRespVO.setDisplayMode(PayOrderDisplayModeEnum.URL.getMode());
        submitRespVO.setDisplayContent("https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=wxh5");
        when(payOrderService.submitOrder(any(PayOrderSubmitReqVO.class), anyString())).thenReturn(submitRespVO);

        CrmebCompatResult<CrmebFrontPayCompatController.CrmebOrderPayResultRespVO> result = controller.payment(reqVO, new MockHttpServletRequest());

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("weixinh5", result.getData().getPayType());
        assertNotNull(result.getData().getJsConfig());
        assertEquals("https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=wxh5",
                result.getData().getJsConfig().get("raw"));
        assertEquals("https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=wxh5",
                result.getData().getJsConfig().get("mwebUrl"));

        ArgumentCaptor<PayOrderSubmitReqVO> captor = ArgumentCaptor.forClass(PayOrderSubmitReqVO.class);
        verify(payOrderService).submitOrder(captor.capture(), anyString());
        assertEquals("wx_wap", captor.getValue().getChannelCode());
    }

    @Test
    void shouldSubmitPaymentWithFromAliasWhenPayChannelMissing() {
        CrmebFrontPayCompatController.CrmebOrderPayRequest reqVO = new CrmebFrontPayCompatController.CrmebOrderPayRequest();
        reqVO.setOrderNo("order_005");
        reqVO.setPayType("WeChat");
        reqVO.setFrom("WeChatH5");

        PayOrderDO order = new PayOrderDO();
        order.setId(5L);
        when(payOrderService.getOrder("order_005")).thenReturn(order);

        PayOrderSubmitRespVO submitRespVO = new PayOrderSubmitRespVO();
        submitRespVO.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        submitRespVO.setDisplayMode(PayOrderDisplayModeEnum.URL.getMode());
        submitRespVO.setDisplayContent("https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=wx005");
        when(payOrderService.submitOrder(any(PayOrderSubmitReqVO.class), anyString())).thenReturn(submitRespVO);

        CrmebCompatResult<CrmebFrontPayCompatController.CrmebOrderPayResultRespVO> result = controller.payment(reqVO, new MockHttpServletRequest());

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("weixinh5", result.getData().getPayType());

        ArgumentCaptor<PayOrderSubmitReqVO> captor = ArgumentCaptor.forClass(PayOrderSubmitReqVO.class);
        verify(payOrderService).submitOrder(captor.capture(), anyString());
        assertEquals("wx_wap", captor.getValue().getChannelCode());
    }

    @Test
    void shouldSubmitPaymentWithWxpayAliasAndUppercaseChannel() {
        CrmebFrontPayCompatController.CrmebOrderPayRequest reqVO = new CrmebFrontPayCompatController.CrmebOrderPayRequest();
        reqVO.setOrderNo("order_006");
        reqVO.setPayType("WxPay");
        reqVO.setPayChannel("WX_APP");

        PayOrderDO order = new PayOrderDO();
        order.setId(6L);
        when(payOrderService.getOrder("order_006")).thenReturn(order);

        PayOrderSubmitRespVO submitRespVO = new PayOrderSubmitRespVO();
        submitRespVO.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        submitRespVO.setDisplayMode(PayOrderDisplayModeEnum.APP.getMode());
        submitRespVO.setDisplayContent("{\"appId\":\"wx006\",\"packageValue\":\"prepay_id=wx006\"}");
        when(payOrderService.submitOrder(any(PayOrderSubmitReqVO.class), anyString())).thenReturn(submitRespVO);

        CrmebCompatResult<CrmebFrontPayCompatController.CrmebOrderPayResultRespVO> result = controller.payment(reqVO, new MockHttpServletRequest());

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("WxPay", result.getData().getPayType());

        ArgumentCaptor<PayOrderSubmitReqVO> captor = ArgumentCaptor.forClass(PayOrderSubmitReqVO.class);
        verify(payOrderService).submitOrder(captor.capture(), anyString());
        assertEquals("wx_app", captor.getValue().getChannelCode());
    }

    @Test
    void shouldQueryPayResultReturnFalseWhenLatestOrderMissingAfterSync() {
        PayOrderDO waitingOrder = new PayOrderDO();
        waitingOrder.setId(7L);
        waitingOrder.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        when(payOrderService.getOrder("order_007")).thenReturn(waitingOrder);
        when(payOrderService.getOrder(7L)).thenReturn(null);

        CrmebCompatResult<Boolean> result = controller.queryPayResult("order_007");

        assertEquals(200, result.getCode());
        assertTrue(Boolean.FALSE.equals(result.getData()));
        verify(payOrderService).syncOrderQuietly(7L);
    }

    @Test
    void shouldQueryPayResultReturnFalseWhenSyncThrowsException() {
        PayOrderDO waitingOrder = new PayOrderDO();
        waitingOrder.setId(8L);
        waitingOrder.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        when(payOrderService.getOrder("order_008")).thenReturn(waitingOrder);
        doThrow(new RuntimeException("sync failed")).when(payOrderService).syncOrderQuietly(8L);
        when(payOrderService.getOrder(8L)).thenReturn(waitingOrder);

        CrmebCompatResult<Boolean> result = controller.queryPayResult("order_008");

        assertEquals(200, result.getCode());
        assertTrue(Boolean.FALSE.equals(result.getData()));
        verify(payOrderService).syncOrderQuietly(8L);
    }

    private void mockLoginUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.MEMBER.getValue());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
