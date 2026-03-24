package cn.iocoder.yudao.module.promotion.service.giftcard;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardOrderCreateReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardOrderCreateRespVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardRefundApplyReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardRefundApplyRespVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardRedeemReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardRedeemRespVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardTemplatePageReqVO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.giftcard.GiftCardDO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.giftcard.GiftCardOrderDO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.giftcard.GiftCardRedeemRecordDO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.giftcard.GiftCardTemplateDO;
import cn.iocoder.yudao.module.promotion.dal.mysql.giftcard.GiftCardMapper;
import cn.iocoder.yudao.module.promotion.dal.mysql.giftcard.GiftCardOrderMapper;
import cn.iocoder.yudao.module.promotion.dal.mysql.giftcard.GiftCardRedeemRecordMapper;
import cn.iocoder.yudao.module.promotion.dal.mysql.giftcard.GiftCardTemplateMapper;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GiftCardServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private GiftCardServiceImpl service;

    @Mock
    private GiftCardTemplateMapper giftCardTemplateMapper;
    @Mock
    private GiftCardOrderMapper giftCardOrderMapper;
    @Mock
    private GiftCardMapper giftCardMapper;
    @Mock
    private GiftCardRedeemRecordMapper giftCardRedeemRecordMapper;

    @Test
    void shouldGetTemplatePage() {
        AppGiftCardTemplatePageReqVO reqVO = new AppGiftCardTemplatePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        GiftCardTemplateDO template = GiftCardTemplateDO.builder()
                .id(101L)
                .title("春日疗愈礼卡")
                .faceValue(9900)
                .stock(20)
                .validDays(30)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(giftCardTemplateMapper.selectAppPage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(template), 1L));

        PageResult<?> result = service.getTemplatePage(reqVO);

        assertEquals(1L, result.getTotal());
        verify(giftCardTemplateMapper).selectAppPage(reqVO);
    }

    @Test
    void shouldCreateOrder() {
        AppGiftCardOrderCreateReqVO reqVO = new AppGiftCardOrderCreateReqVO();
        reqVO.setTemplateId(101L);
        reqVO.setQuantity(2);
        reqVO.setSendScene("SELF");
        reqVO.setClientToken("gift-create-001");
        GiftCardTemplateDO template = GiftCardTemplateDO.builder()
                .id(101L)
                .title("春日疗愈礼卡")
                .faceValue(9900)
                .stock(20)
                .validDays(30)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(giftCardTemplateMapper.selectById(101L)).thenReturn(template);

        AppGiftCardOrderCreateRespVO result = service.createOrder(66L, reqVO);

        assertEquals(19800, result.getAmountTotal());
        assertFalse(result.getDegraded());
        verify(giftCardOrderMapper).insert(any(GiftCardOrderDO.class));
        verify(giftCardMapper, times(2)).insert(any(GiftCardDO.class));
        verify(giftCardTemplateMapper).updateStock(101L, -2);
    }

    @Test
    void shouldRedeem() {
        AppGiftCardRedeemReqVO reqVO = new AppGiftCardRedeemReqVO();
        reqVO.setCardNo("GC1001");
        reqVO.setRedeemCode("8888");
        reqVO.setClientToken("gift-redeem-001");
        GiftCardDO card = GiftCardDO.builder()
                .id(3001L)
                .cardNo("GC1001")
                .orderId(9001L)
                .receiverMemberId(66L)
                .status("ISSUED")
                .redeemCode("8888")
                .build();
        when(giftCardMapper.selectByCardNo("GC1001")).thenReturn(card);

        AppGiftCardRedeemRespVO result = service.redeem(66L, reqVO);

        assertEquals("REDEEMED", result.getCardStatus());
        assertFalse(result.getDegraded());
        verify(giftCardRedeemRecordMapper).insert(any(GiftCardRedeemRecordDO.class));
        verify(giftCardMapper).updateById(any(GiftCardDO.class));
    }

    @Test
    void shouldApplyRefund() {
        AppGiftCardRefundApplyReqVO reqVO = new AppGiftCardRefundApplyReqVO();
        reqVO.setOrderId(9001L);
        reqVO.setReason("未使用");
        reqVO.setClientToken("gift-refund-001");
        GiftCardOrderDO order = GiftCardOrderDO.builder()
                .id(9001L)
                .memberId(66L)
                .status("ISSUED")
                .amountTotal(19800)
                .build();
        when(giftCardOrderMapper.selectById(9001L)).thenReturn(order);
        when(giftCardMapper.selectListByOrderId(9001L)).thenReturn(Collections.singletonList(
                GiftCardDO.builder().id(3001L).orderId(9001L).status("ISSUED").build()
        ));

        AppGiftCardRefundApplyRespVO result = service.applyRefund(66L, reqVO);

        assertEquals("REFUND_PENDING", result.getRefundStatus());
        assertTrue(result.getAfterSaleId() > 0);
        verify(giftCardOrderMapper).updateById(any(GiftCardOrderDO.class));
        verify(giftCardMapper).markOrderCardsRefundPending(9001L, "REFUND_PENDING");
    }
}
