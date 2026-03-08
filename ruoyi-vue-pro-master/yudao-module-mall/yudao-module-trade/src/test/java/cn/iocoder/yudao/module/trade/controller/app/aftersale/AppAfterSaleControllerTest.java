package cn.iocoder.yudao.module.trade.controller.app.aftersale;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.api.refund.PayRefundApi;
import cn.iocoder.yudao.module.pay.api.refund.dto.PayRefundRespDTO;
import cn.iocoder.yudao.module.pay.enums.refund.PayRefundStatusEnum;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSaleRefundProgressRespVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleStatusEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;

import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppAfterSaleControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppAfterSaleController controller;

    @Mock
    private AfterSaleService afterSaleService;
    @Mock
    private PayRefundApi payRefundApi;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getRefundProgress_shouldReturnFullPayloadWhenAfterSaleAndPayRefundExists() {
        mockLoginUser(101L);
        Long afterSaleId = 7001L;
        Long payRefundId = 4001L;
        LocalDateTime payRefundSuccessTime = LocalDateTime.of(2026, 3, 8, 11, 20, 0);

        AfterSaleDO afterSale = new AfterSaleDO();
        afterSale.setId(afterSaleId);
        afterSale.setNo("AS202603080001");
        afterSale.setOrderId(2001L);
        afterSale.setOrderNo("T202603080001");
        afterSale.setStatus(AfterSaleStatusEnum.WAIT_REFUND.getStatus());
        afterSale.setRefundPrice(1200);
        afterSale.setPayRefundId(payRefundId);
        afterSale.setRefundTime(null);
        when(afterSaleService.getAfterSale(eq(101L), eq(afterSaleId))).thenReturn(afterSale);

        PayRefundRespDTO payRefund = new PayRefundRespDTO();
        payRefund.setId(payRefundId);
        payRefund.setStatus(PayRefundStatusEnum.WAITING.getStatus());
        payRefund.setMerchantOrderId("2001");
        payRefund.setMerchantRefundId("2001-refund");
        payRefund.setChannelErrorCode("CHANNEL_BUSY");
        payRefund.setChannelErrorMsg("系统繁忙");
        payRefund.setSuccessTime(payRefundSuccessTime);
        when(payRefundApi.getRefund(eq(payRefundId))).thenReturn(payRefund);

        CommonResult<AppAfterSaleRefundProgressRespVO> result = controller.getRefundProgress(null, afterSaleId);

        assertTrue(result.isSuccess());
        AppAfterSaleRefundProgressRespVO data = result.getData();
        assertNotNull(data);
        assertEquals(afterSaleId, data.getAfterSaleId());
        assertEquals("AS202603080001", data.getAfterSaleNo());
        assertEquals(2001L, data.getOrderId());
        assertEquals("T202603080001", data.getOrderNo());
        assertEquals(AfterSaleStatusEnum.WAIT_REFUND.getStatus(), data.getAfterSaleStatus());
        assertEquals(AfterSaleStatusEnum.WAIT_REFUND.getName(), data.getAfterSaleStatusName());
        assertEquals(1200, data.getRefundPrice());
        assertEquals(payRefundId, data.getPayRefundId());
        assertEquals(PayRefundStatusEnum.WAITING.getStatus(), data.getPayRefundStatus());
        assertEquals(PayRefundStatusEnum.WAITING.getName(), data.getPayRefundStatusName());
        assertEquals("2001", data.getMerchantOrderId());
        assertEquals("2001-refund", data.getMerchantRefundId());
        assertEquals("CHANNEL_BUSY", data.getChannelErrorCode());
        assertEquals("系统繁忙", data.getChannelErrorMsg());
        assertEquals(payRefundSuccessTime, data.getRefundTime());
        assertEquals("REFUND_PROCESSING", data.getProgressCode());
        assertEquals("退款处理中", data.getProgressDesc());
    }

    @Test
    void getRefundProgress_shouldReturnNullWhenAfterSaleNotExists() {
        mockLoginUser(101L);
        Long orderId = 8001L;
        when(afterSaleService.getLatestAfterSaleByOrderId(eq(101L), eq(orderId))).thenReturn(null);

        CommonResult<AppAfterSaleRefundProgressRespVO> result = controller.getRefundProgress(orderId, null);

        assertTrue(result.isSuccess());
        assertNull(result.getData());
        verifyNoInteractions(payRefundApi);
    }

    @Test
    void getRefundProgress_shouldReturnNullWhenParamsEmpty() {
        CommonResult<AppAfterSaleRefundProgressRespVO> result = controller.getRefundProgress(null, null);

        assertTrue(result.isSuccess());
        assertNull(result.getData());
        verifyNoInteractions(afterSaleService);
        verifyNoInteractions(payRefundApi);
    }

    @Test
    void errorCodeAnchor_shouldKeepAfterSaleBundleRefundCodeStable() {
        assertEquals(1_011_000_125, AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED.getCode());
    }

    private void mockLoginUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.MEMBER.getValue());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
