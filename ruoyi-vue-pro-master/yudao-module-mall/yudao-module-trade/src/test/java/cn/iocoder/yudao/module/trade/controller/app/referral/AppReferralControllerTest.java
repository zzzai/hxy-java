package cn.iocoder.yudao.module.trade.controller.app.referral;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.dal.dataobject.brokerage.BrokerageRecordDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.brokerage.BrokerageUserDO;
import cn.iocoder.yudao.module.trade.service.brokerage.BrokerageRecordService;
import cn.iocoder.yudao.module.trade.service.brokerage.BrokerageUserService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppReferralControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppReferralController controller;

    @Mock
    private BrokerageUserService brokerageUserService;
    @Mock
    private BrokerageRecordService brokerageRecordService;

    @Test
    void shouldBindInviter() {
        AppReferralBindReqVO reqVO = new AppReferralBindReqVO();
        reqVO.setInviterMemberId(88L);
        reqVO.setClientToken("bind-001");
        when(brokerageUserService.bindBrokerageUser(66L, 88L)).thenReturn(true);

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);

            CommonResult<AppReferralBindRespVO> result = controller.bindInviter(reqVO);

            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals(66L, result.getData().getRefereeMemberId());
            assertEquals(88L, result.getData().getInviterMemberId());
            assertEquals("BOUND", result.getData().getBindStatus());
        }

        verify(brokerageUserService).bindBrokerageUser(66L, 88L);
    }

    @Test
    void shouldGetOverview() {
        BrokerageUserDO brokerageUser = BrokerageUserDO.builder()
                .id(66L)
                .bindUserId(12L)
                .brokeragePrice(500)
                .frozenPrice(200)
                .build();
        when(brokerageUserService.getOrCreateBrokerageUser(66L)).thenReturn(brokerageUser);
        when(brokerageUserService.getBrokerageUserCountByBindUserId(66L, 1)).thenReturn(3L);
        when(brokerageUserService.getBrokerageUserCountByBindUserId(66L, 2)).thenReturn(5L);

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);

            CommonResult<AppReferralOverviewRespVO> result = controller.getOverview();

            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals("66", result.getData().getReferralCode());
            assertEquals(8L, result.getData().getTotalInvites());
            assertEquals(3L, result.getData().getEffectiveInvites());
            assertEquals(200, result.getData().getPendingRewardAmount());
            assertEquals(500, result.getData().getRewardBalance());
        }
    }

    @Test
    void shouldGetRewardLedgerPage() {
        AppReferralRewardLedgerPageReqVO reqVO = new AppReferralRewardLedgerPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        BrokerageRecordDO row = BrokerageRecordDO.builder()
                .id(9001L)
                .bizId("ORDER-1")
                .price(180)
                .status(10)
                .build();
        when(brokerageRecordService.getBrokerageRecordPage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);

            CommonResult<PageResult<AppReferralRewardLedgerRespVO>> result = controller.getRewardLedgerPage(reqVO);

            assertTrue(result.isSuccess());
            assertEquals(1L, result.getData().getTotal());
            assertEquals(1, result.getData().getList().size());
            assertEquals(9001L, result.getData().getList().get(0).getLedgerId());
        }
    }
}
