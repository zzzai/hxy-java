package cn.iocoder.yudao.module.pay.controller.compat.crmeb;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import cn.iocoder.yudao.module.pay.controller.admin.order.vo.PayOrderSubmitRespVO;
import cn.iocoder.yudao.module.pay.dal.dataobject.order.PayOrderDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.wallet.PayWalletRechargeDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.wallet.PayWalletRechargePackageDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.wallet.PayWalletTransactionDO;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.pay.framework.pay.core.enums.PayOrderDisplayModeEnum;
import cn.iocoder.yudao.module.pay.service.order.PayOrderService;
import cn.iocoder.yudao.module.pay.service.wallet.PayWalletRechargePackageService;
import cn.iocoder.yudao.module.pay.service.wallet.PayWalletRechargeService;
import cn.iocoder.yudao.module.pay.service.wallet.PayWalletTransactionService;
import cn.iocoder.yudao.module.system.api.social.SocialUserApi;
import cn.iocoder.yudao.module.system.api.social.dto.SocialUserRespDTO;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.trade.api.brokerage.TradeBrokerageApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrmebFrontRechargeCompatControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private CrmebFrontRechargeCompatController controller;

    @Mock
    private PayWalletRechargePackageService payWalletRechargePackageService;
    @Mock
    private PayWalletRechargeService payWalletRechargeService;
    @Mock
    private PayOrderService payOrderService;
    @Mock
    private PayWalletTransactionService payWalletTransactionService;
    @Mock
    private SocialUserApi socialUserApi;
    @Mock
    private TradeBrokerageApi tradeBrokerageApi;

    @Test
    void shouldReturnRechargeConfig() {
        mockLoginUser(100L);
        PayWalletRechargePackageDO pkg = new PayWalletRechargePackageDO();
        pkg.setId(11L);
        pkg.setPayPrice(1000);
        pkg.setBonusPrice(200);
        when(payWalletRechargePackageService.getWalletRechargePackageList(anyInt())).thenReturn(List.of(pkg));

        CrmebCompatResult<CrmebFrontRechargeCompatController.CrmebRechargeIndexRespVO> result = controller.getRechargeConfig();

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getRechargeQuota().size());
        assertEquals("10.00", result.getData().getRechargeQuota().get(0).getPrice());
        assertEquals("2.00", result.getData().getRechargeQuota().get(0).getGiveMoney());
        assertTrue(result.getData().getRechargeAttention().isEmpty());
    }

    @Test
    void shouldRoutineRechargeAndReturnPayConfig() {
        mockLoginUser(200L);
        CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO reqVO = new CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO();
        reqVO.setPrice(new BigDecimal("12.34"));
        reqVO.setRecharId(0L);

        SocialUserRespDTO socialUser = new SocialUserRespDTO();
        socialUser.setOpenid("openid_200");
        when(socialUserApi.getSocialUserByUserId(UserTypeEnum.MEMBER.getValue(), 200L,
                SocialTypeEnum.WECHAT_MINI_PROGRAM.getType())).thenReturn(socialUser);

        PayWalletRechargeDO rechargeDO = new PayWalletRechargeDO();
        rechargeDO.setId(66L);
        rechargeDO.setPayOrderId(88L);
        when(payWalletRechargeService.createWalletRecharge(eq(200L), eq(UserTypeEnum.MEMBER.getValue()),
                anyString(), any())).thenReturn(rechargeDO);

        PayOrderSubmitRespVO submitRespVO = new PayOrderSubmitRespVO();
        submitRespVO.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        submitRespVO.setDisplayMode(PayOrderDisplayModeEnum.APP.getMode());
        submitRespVO.setDisplayContent("{\"packageValue\":\"prepay_id=wx123\",\"timeStamp\":\"1\"}");
        when(payOrderService.submitOrder(any(), anyString())).thenReturn(submitRespVO);

        PayOrderDO payOrderDO = new PayOrderDO();
        payOrderDO.setNo("PAY202602190001");
        when(payOrderService.getOrder(88L)).thenReturn(payOrderDO);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        CrmebCompatResult<Map<String, Object>> result = controller.routineRecharge(reqVO, request);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("routine", result.getData().get("type"));
        Object dataObj = result.getData().get("data");
        assertTrue(dataObj instanceof CrmebFrontRechargeCompatController.CrmebRechargePayResultRespVO);
        CrmebFrontRechargeCompatController.CrmebRechargePayResultRespVO payResult =
                (CrmebFrontRechargeCompatController.CrmebRechargePayResultRespVO) dataObj;
        assertTrue(Boolean.TRUE.equals(payResult.getStatus()));
        assertEquals("weixin", payResult.getPayType());
        assertEquals("PAY202602190001", payResult.getOrderNo());
        assertNotNull(payResult.getJsConfig());
        assertEquals("prepay_id=wx123", payResult.getJsConfig().get("package"));
        assertEquals("prepay_id=wx123", payResult.getJsConfig().get("packages"));
    }

    @Test
    void shouldFailRoutineRechargeWhenOpenidMissing() {
        mockLoginUser(300L);
        CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO reqVO = new CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO();
        reqVO.setPrice(new BigDecimal("10"));
        reqVO.setRecharId(0L);
        when(socialUserApi.getSocialUserByUserId(UserTypeEnum.MEMBER.getValue(), 300L,
                SocialTypeEnum.WECHAT_MINI_PROGRAM.getType())).thenReturn(null);

        CrmebCompatResult<Map<String, Object>> result = controller.routineRecharge(reqVO, new MockHttpServletRequest());

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("微信授权"));
    }

    @Test
    void shouldWechatRechargeReturnMwebUrlWhenWapMode() {
        mockLoginUser(400L);
        CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO reqVO = new CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO();
        reqVO.setPrice(new BigDecimal("8.88"));
        reqVO.setRecharId(0L);
        reqVO.setFrom("weixinh5");

        PayWalletRechargeDO rechargeDO = new PayWalletRechargeDO();
        rechargeDO.setId(77L);
        rechargeDO.setPayOrderId(99L);
        when(payWalletRechargeService.createWalletRecharge(eq(400L), eq(UserTypeEnum.MEMBER.getValue()),
                anyString(), any())).thenReturn(rechargeDO);

        PayOrderSubmitRespVO submitRespVO = new PayOrderSubmitRespVO();
        submitRespVO.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        submitRespVO.setDisplayMode(PayOrderDisplayModeEnum.URL.getMode());
        submitRespVO.setDisplayContent("https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=wx123");
        when(payOrderService.submitOrder(any(), anyString())).thenReturn(submitRespVO);

        PayOrderDO payOrderDO = new PayOrderDO();
        payOrderDO.setNo("PAY202602190099");
        when(payOrderService.getOrder(99L)).thenReturn(payOrderDO);

        CrmebCompatResult<CrmebFrontRechargeCompatController.CrmebRechargePayResultRespVO> result =
                controller.wechatRecharge(reqVO, new MockHttpServletRequest());

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertNotNull(result.getData().getJsConfig());
        assertEquals("https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=wx123",
                result.getData().getJsConfig().get("mwebUrl"));
    }

    @Test
    void shouldWechatAppRechargeNormalizeAppFields() {
        mockLoginUser(500L);
        CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO reqVO = new CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO();
        reqVO.setPrice(new BigDecimal("9.99"));
        reqVO.setRecharId(0L);

        PayWalletRechargeDO rechargeDO = new PayWalletRechargeDO();
        rechargeDO.setId(78L);
        rechargeDO.setPayOrderId(100L);
        when(payWalletRechargeService.createWalletRecharge(eq(500L), eq(UserTypeEnum.MEMBER.getValue()),
                anyString(), any())).thenReturn(rechargeDO);

        PayOrderSubmitRespVO submitRespVO = new PayOrderSubmitRespVO();
        submitRespVO.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        submitRespVO.setDisplayMode(PayOrderDisplayModeEnum.APP.getMode());
        submitRespVO.setDisplayContent("{\"appId\":\"wxabc\",\"partnerId\":\"1900000109\",\"prepayId\":\"wxprep123\",\"timeStamp\":\"1\"}");
        when(payOrderService.submitOrder(any(), anyString())).thenReturn(submitRespVO);

        PayOrderDO payOrderDO = new PayOrderDO();
        payOrderDO.setNo("PAY202602190100");
        when(payOrderService.getOrder(100L)).thenReturn(payOrderDO);

        CrmebCompatResult<CrmebFrontRechargeCompatController.CrmebRechargePayResultRespVO> result =
                controller.wechatAppRecharge(reqVO, new MockHttpServletRequest());

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertNotNull(result.getData().getJsConfig());
        assertEquals("1900000109", result.getData().getJsConfig().get("partnerid"));
        assertEquals("wxprep123", result.getData().getJsConfig().get("packages"));
    }

    @Test
    void shouldWechatRechargeSupportWechatAliasWithMpOpenid() {
        mockLoginUser(410L);
        CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO reqVO = new CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO();
        reqVO.setPrice(new BigDecimal("6.66"));
        reqVO.setRecharId(0L);
        reqVO.setFrom("wechat");

        SocialUserRespDTO socialUser = new SocialUserRespDTO();
        socialUser.setOpenid("openid_mp_410");
        when(socialUserApi.getSocialUserByUserId(UserTypeEnum.MEMBER.getValue(), 410L,
                SocialTypeEnum.WECHAT_MP.getType())).thenReturn(socialUser);

        PayWalletRechargeDO rechargeDO = new PayWalletRechargeDO();
        rechargeDO.setId(79L);
        rechargeDO.setPayOrderId(101L);
        when(payWalletRechargeService.createWalletRecharge(eq(410L), eq(UserTypeEnum.MEMBER.getValue()),
                anyString(), any())).thenReturn(rechargeDO);

        PayOrderSubmitRespVO submitRespVO = new PayOrderSubmitRespVO();
        submitRespVO.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        submitRespVO.setDisplayMode(PayOrderDisplayModeEnum.APP.getMode());
        submitRespVO.setDisplayContent("{\"packageValue\":\"prepay_id=wx410\",\"timeStamp\":\"1\"}");
        when(payOrderService.submitOrder(any(), anyString())).thenReturn(submitRespVO);

        PayOrderDO payOrderDO = new PayOrderDO();
        payOrderDO.setNo("PAY202602190410");
        when(payOrderService.getOrder(101L)).thenReturn(payOrderDO);

        CrmebCompatResult<CrmebFrontRechargeCompatController.CrmebRechargePayResultRespVO> result =
                controller.wechatRecharge(reqVO, new MockHttpServletRequest());

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());

        ArgumentCaptor<PayOrderSubmitReqVO> submitCaptor = ArgumentCaptor.forClass(PayOrderSubmitReqVO.class);
        verify(payOrderService).submitOrder(submitCaptor.capture(), anyString());
        assertEquals("wx_pub", submitCaptor.getValue().getChannelCode());
        assertNotNull(submitCaptor.getValue().getChannelExtras());
        assertEquals("openid_mp_410", submitCaptor.getValue().getChannelExtras().get("openid"));
    }

    @Test
    void shouldWechatRechargeSupportMixedCaseH5Alias() {
        mockLoginUser(411L);
        CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO reqVO = new CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO();
        reqVO.setPrice(new BigDecimal("5.55"));
        reqVO.setRecharId(0L);
        reqVO.setFrom("WeChatH5");

        PayWalletRechargeDO rechargeDO = new PayWalletRechargeDO();
        rechargeDO.setId(80L);
        rechargeDO.setPayOrderId(102L);
        when(payWalletRechargeService.createWalletRecharge(eq(411L), eq(UserTypeEnum.MEMBER.getValue()),
                anyString(), any())).thenReturn(rechargeDO);

        PayOrderSubmitRespVO submitRespVO = new PayOrderSubmitRespVO();
        submitRespVO.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        submitRespVO.setDisplayMode(PayOrderDisplayModeEnum.URL.getMode());
        submitRespVO.setDisplayContent("https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=wx411");
        when(payOrderService.submitOrder(any(), anyString())).thenReturn(submitRespVO);

        PayOrderDO payOrderDO = new PayOrderDO();
        payOrderDO.setNo("PAY202602190411");
        when(payOrderService.getOrder(102L)).thenReturn(payOrderDO);

        CrmebCompatResult<CrmebFrontRechargeCompatController.CrmebRechargePayResultRespVO> result =
                controller.wechatRecharge(reqVO, new MockHttpServletRequest());

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());

        ArgumentCaptor<PayOrderSubmitReqVO> submitCaptor = ArgumentCaptor.forClass(PayOrderSubmitReqVO.class);
        verify(payOrderService).submitOrder(submitCaptor.capture(), anyString());
        assertEquals("wx_wap", submitCaptor.getValue().getChannelCode());
    }

    @Test
    void shouldWechatRechargeFallbackToPayTypeWhenFromMissing() {
        mockLoginUser(412L);
        CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO reqVO = new CrmebFrontRechargeCompatController.CrmebRechargeRoutineReqVO();
        reqVO.setPrice(new BigDecimal("4.44"));
        reqVO.setRecharId(0L);
        reqVO.setPayType("MP");

        SocialUserRespDTO socialUser = new SocialUserRespDTO();
        socialUser.setOpenid("openid_mp_412");
        when(socialUserApi.getSocialUserByUserId(UserTypeEnum.MEMBER.getValue(), 412L,
                SocialTypeEnum.WECHAT_MP.getType())).thenReturn(socialUser);

        PayWalletRechargeDO rechargeDO = new PayWalletRechargeDO();
        rechargeDO.setId(81L);
        rechargeDO.setPayOrderId(103L);
        when(payWalletRechargeService.createWalletRecharge(eq(412L), eq(UserTypeEnum.MEMBER.getValue()),
                anyString(), any())).thenReturn(rechargeDO);

        PayOrderSubmitRespVO submitRespVO = new PayOrderSubmitRespVO();
        submitRespVO.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        submitRespVO.setDisplayMode(PayOrderDisplayModeEnum.APP.getMode());
        submitRespVO.setDisplayContent("{\"packageValue\":\"prepay_id=wx412\",\"timeStamp\":\"1\"}");
        when(payOrderService.submitOrder(any(), anyString())).thenReturn(submitRespVO);

        PayOrderDO payOrderDO = new PayOrderDO();
        payOrderDO.setNo("PAY202602190412");
        when(payOrderService.getOrder(103L)).thenReturn(payOrderDO);

        CrmebCompatResult<CrmebFrontRechargeCompatController.CrmebRechargePayResultRespVO> result =
                controller.wechatRecharge(reqVO, new MockHttpServletRequest());

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());

        ArgumentCaptor<PayOrderSubmitReqVO> submitCaptor = ArgumentCaptor.forClass(PayOrderSubmitReqVO.class);
        verify(payOrderService).submitOrder(submitCaptor.capture(), anyString());
        assertEquals("wx_pub", submitCaptor.getValue().getChannelCode());
        assertNotNull(submitCaptor.getValue().getChannelExtras());
        assertEquals("openid_mp_412", submitCaptor.getValue().getChannelExtras().get("openid"));
    }

    @Test
    void shouldTransferInSuccess() {
        mockLoginUser(600L);
        CrmebFrontRechargeCompatController.CrmebTransferInReqVO reqVO = new CrmebFrontRechargeCompatController.CrmebTransferInReqVO();
        reqVO.setPrice(new BigDecimal("1.23"));

        CrmebCompatResult<Boolean> result = controller.transferIn(null, reqVO);

        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(tradeBrokerageApi).transferIn(600L, 123);
    }

    @Test
    void shouldReturnBillRecord() {
        mockLoginUser(700L);
        PayWalletTransactionDO income = new PayWalletTransactionDO();
        income.setTitle("充值");
        income.setPrice(100);
        income.setCreateTime(LocalDateTime.of(2026, 2, 19, 10, 30, 0));
        PayWalletTransactionDO expense = new PayWalletTransactionDO();
        expense.setTitle("支付");
        expense.setPrice(-50);
        expense.setCreateTime(LocalDateTime.of(2026, 2, 18, 9, 0, 0));

        when(payWalletTransactionService.getWalletTransactionPage(eq(700L), eq(UserTypeEnum.MEMBER.getValue()), any()))
                .thenReturn(new PageResult<>(List.of(income, expense), 2L));

        CrmebCompatResult<CrmebFrontRechargeCompatController.CrmebPageRespVO<CrmebFrontRechargeCompatController.CrmebRechargeBillRecordRespVO>> result =
                controller.billRecord("all", 1, 12);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getTotalPage());
        assertEquals(1, result.getData().getList().size());
        CrmebFrontRechargeCompatController.CrmebRechargeBillRecordRespVO monthGroup = result.getData().getList().get(0);
        assertEquals("2026-02", monthGroup.getDate());
        assertEquals(2, monthGroup.getList().size());
        assertEquals(1, monthGroup.getList().get(0).getPm());
        assertEquals("1.00", monthGroup.getList().get(0).getNumber());
        assertEquals(0, monthGroup.getList().get(1).getPm());
        assertEquals("0.50", monthGroup.getList().get(1).getNumber());
    }

    @Test
    void shouldRejectRechargeConfigWhenNotLogin() {
        SecurityContextHolder.clearContext();

        CrmebCompatResult<CrmebFrontRechargeCompatController.CrmebRechargeIndexRespVO> result = controller.getRechargeConfig();

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("请先登录"));
    }

    private void mockLoginUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.MEMBER.getValue());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
