package cn.iocoder.yudao.server.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.member.controller.app.point.vo.AppMemberPointRecordPageReqVO;
import cn.iocoder.yudao.module.member.dal.dataobject.point.MemberPointRecordDO;
import cn.iocoder.yudao.module.member.service.point.MemberPointRecordService;
import cn.iocoder.yudao.module.pay.controller.app.wallet.vo.transaction.AppPayWalletTransactionPageReqVO;
import cn.iocoder.yudao.module.pay.dal.dataobject.wallet.PayWalletTransactionDO;
import cn.iocoder.yudao.module.pay.service.wallet.PayWalletTransactionService;
import cn.iocoder.yudao.module.promotion.dal.dataobject.coupon.CouponDO;
import cn.iocoder.yudao.module.promotion.service.coupon.CouponService;
import cn.iocoder.yudao.server.controller.app.member.vo.AppMemberAssetLedgerPageReqVO;
import cn.iocoder.yudao.server.controller.app.member.vo.AppMemberAssetLedgerPageRespVO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AppMemberAssetLedgerServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppMemberAssetLedgerService service;

    @Mock
    private MemberPointRecordService memberPointRecordService;
    @Mock
    private PayWalletTransactionService payWalletTransactionService;
    @Mock
    private CouponService couponService;

    @Test
    void getAssetLedgerPage_shouldMergeWalletPointAndCouponRecords() {
        AppMemberAssetLedgerPageReqVO reqVO = new AppMemberAssetLedgerPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PayWalletTransactionDO wallet = new PayWalletTransactionDO();
        wallet.setId(11L);
        wallet.setBizType(3);
        wallet.setBizId("PAY-11");
        wallet.setNo("WALLET-11");
        wallet.setPrice(8800);
        wallet.setBalance(18800);
        wallet.setCreateTime(LocalDateTime.of(2026, 3, 24, 11, 0, 0));
        when(payWalletTransactionService.getWalletTransactionPage(eq(66L), eq(1), any(AppPayWalletTransactionPageReqVO.class)))
                .thenReturn(new PageResult<>(Arrays.asList(wallet), 1L));

        MemberPointRecordDO point = new MemberPointRecordDO();
        point.setId(22L);
        point.setBizType(4);
        point.setBizId("POINT-22");
        point.setPoint(200);
        point.setTotalPoint(5200);
        point.setCreateTime(LocalDateTime.of(2026, 3, 24, 10, 0, 0));
        when(memberPointRecordService.getPointRecordPage(eq(66L), any(AppMemberPointRecordPageReqVO.class)))
                .thenReturn(new PageResult<>(Arrays.asList(point), 1L));

        CouponDO coupon = new CouponDO();
        coupon.setId(33L);
        coupon.setTemplateId(303L);
        coupon.setStatus(1);
        coupon.setDiscountPrice(1500);
        coupon.setCreateTime(LocalDateTime.of(2026, 3, 24, 9, 0, 0));
        when(couponService.getCouponPage(any())).thenReturn(new PageResult<>(Arrays.asList(coupon), 1L));

        AppMemberAssetLedgerPageRespVO result = service.getAssetLedgerPage(66L, reqVO);

        assertNotNull(result);
        assertFalse(result.getDegraded());
        assertEquals(3L, result.getTotal());
        assertEquals(3, result.getList().size());
        assertEquals("WALLET", result.getList().get(0).getAssetType());
        assertEquals("POINT", result.getList().get(1).getAssetType());
        assertEquals("COUPON", result.getList().get(2).getAssetType());
        assertEquals("PAY-11", result.getList().get(0).getSourceBizNo());
        assertEquals("POINT-22", result.getList().get(1).getSourceBizNo());
        assertEquals("303", result.getList().get(2).getSourceBizNo());
    }
}
