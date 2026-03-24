package cn.iocoder.yudao.module.promotion.controller.app.giftcard;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.promotion.service.giftcard.GiftCardService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppGiftCardControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppGiftCardController controller;

    @Mock
    private GiftCardService giftCardService;

    @Test
    void shouldGetTemplatePage() {
        AppGiftCardTemplatePageReqVO reqVO = new AppGiftCardTemplatePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        AppGiftCardTemplateRespVO row = new AppGiftCardTemplateRespVO()
                .setTemplateId(101L)
                .setTitle("春日疗愈礼卡");
        when(giftCardService.getTemplatePage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        CommonResult<PageResult<AppGiftCardTemplateRespVO>> result = controller.getTemplatePage(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(101L, result.getData().getList().get(0).getTemplateId());
        verify(giftCardService).getTemplatePage(reqVO);
    }

    @Test
    void shouldCreateOrder() {
        AppGiftCardOrderCreateReqVO reqVO = new AppGiftCardOrderCreateReqVO();
        reqVO.setTemplateId(101L);
        reqVO.setQuantity(2);
        reqVO.setSendScene("SELF");
        reqVO.setClientToken("gift-create-001");
        AppGiftCardOrderCreateRespVO respVO = new AppGiftCardOrderCreateRespVO()
                .setOrderId(9001L)
                .setGiftCardBatchNo("GIFT-20260324-001")
                .setAmountTotal(19900)
                .setDegraded(false);
        when(giftCardService.createOrder(66L, reqVO)).thenReturn(respVO);

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);

            CommonResult<AppGiftCardOrderCreateRespVO> result = controller.createOrder(reqVO);

            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals(9001L, result.getData().getOrderId());
        }

        verify(giftCardService).createOrder(66L, reqVO);
    }

    @Test
    void shouldGetOrder() {
        AppGiftCardOrderRespVO.CardItem card = new AppGiftCardOrderRespVO.CardItem()
                .setCardNo("GC1001")
                .setStatus("ISSUED")
                .setReceiverMemberId(66L);
        AppGiftCardOrderRespVO respVO = new AppGiftCardOrderRespVO()
                .setOrderId(9001L)
                .setStatus("ISSUED")
                .setDegraded(false)
                .setCards(Collections.singletonList(card));
        when(giftCardService.getOrder(66L, 9001L)).thenReturn(respVO);

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);

            CommonResult<AppGiftCardOrderRespVO> result = controller.getOrder(9001L);

            assertTrue(result.isSuccess());
            assertEquals("ISSUED", result.getData().getStatus());
            assertEquals(1, result.getData().getCards().size());
        }
    }

    @Test
    void shouldRedeem() {
        AppGiftCardRedeemReqVO reqVO = new AppGiftCardRedeemReqVO();
        reqVO.setCardNo("GC1001");
        reqVO.setRedeemCode("8888");
        reqVO.setClientToken("gift-redeem-001");
        AppGiftCardRedeemRespVO respVO = new AppGiftCardRedeemRespVO()
                .setRedeemRecordId(7001L)
                .setMemberId(66L)
                .setCardStatus("REDEEMED")
                .setDegraded(false);
        when(giftCardService.redeem(66L, reqVO)).thenReturn(respVO);

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);

            CommonResult<AppGiftCardRedeemRespVO> result = controller.redeem(reqVO);

            assertTrue(result.isSuccess());
            assertEquals("REDEEMED", result.getData().getCardStatus());
        }
    }

    @Test
    void shouldApplyRefund() {
        AppGiftCardRefundApplyReqVO reqVO = new AppGiftCardRefundApplyReqVO();
        reqVO.setOrderId(9001L);
        reqVO.setReason("未使用");
        reqVO.setClientToken("gift-refund-001");
        AppGiftCardRefundApplyRespVO respVO = new AppGiftCardRefundApplyRespVO()
                .setAfterSaleId(6001L)
                .setRefundStatus("REFUND_PENDING")
                .setDegraded(false);
        when(giftCardService.applyRefund(66L, reqVO)).thenReturn(respVO);

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);

            CommonResult<AppGiftCardRefundApplyRespVO> result = controller.applyRefund(reqVO);

            assertTrue(result.isSuccess());
            assertEquals("REFUND_PENDING", result.getData().getRefundStatus());
        }
    }
}
