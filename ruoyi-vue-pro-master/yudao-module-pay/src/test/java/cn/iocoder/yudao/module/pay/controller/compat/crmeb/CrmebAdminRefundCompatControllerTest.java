package cn.iocoder.yudao.module.pay.controller.compat.crmeb;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.dal.dataobject.refund.PayRefundDO;
import cn.iocoder.yudao.module.pay.dal.mysql.refund.PayRefundMapper;
import cn.iocoder.yudao.module.pay.enums.refund.PayRefundStatusEnum;
import cn.iocoder.yudao.module.pay.service.refund.PayRefundService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class CrmebAdminRefundCompatControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private CrmebAdminRefundCompatController controller;

    @Mock
    private PayRefundService payRefundService;
    @Mock
    private PayRefundMapper payRefundMapper;

    @Test
    void shouldQueryRefundByOrderNo() {
        PayRefundDO refund = new PayRefundDO();
        refund.setId(10L);
        refund.setNo("rf_001");
        refund.setMerchantOrderId("ORDER_001");
        refund.setMerchantRefundId("MR_001");
        refund.setStatus(PayRefundStatusEnum.SUCCESS.getStatus());
        refund.setPayPrice(100);
        refund.setRefundPrice(100);
        when(payRefundMapper.selectFirstByMerchantOrderId("ORDER_001")).thenReturn(refund);

        CrmebCompatResult<CrmebAdminRefundCompatController.CrmebRefundQueryRespVO> result =
                controller.query(null, null, "ORDER_001", null, null);

        assertEquals(200, result.getCode());
        assertEquals(10L, result.getData().getPayRefundId());
        assertEquals("退款成功", result.getData().getStatusText());
        assertEquals(2, result.getData().getRefundStatus());
    }

    @Test
    void shouldQueryRefundByRefundOrderNoFallbackMerchantRefundId() {
        when(payRefundService.getRefundByNo("MR_002")).thenReturn(null);
        PayRefundDO refund = new PayRefundDO();
        refund.setId(11L);
        refund.setNo("rf_002");
        refund.setMerchantOrderId("ORDER_002");
        refund.setMerchantRefundId("MR_002");
        refund.setStatus(PayRefundStatusEnum.WAITING.getStatus());
        refund.setPayPrice(200);
        refund.setRefundPrice(100);
        when(payRefundMapper.selectFirstByMerchantRefundId("MR_002")).thenReturn(refund);

        CrmebCompatResult<CrmebAdminRefundCompatController.CrmebRefundQueryRespVO> result =
                controller.query(null, null, null, "MR_002", null);

        assertEquals(200, result.getCode());
        assertEquals(11L, result.getData().getPayRefundId());
        assertEquals("退款处理中", result.getData().getStatusText());
        assertEquals(1, result.getData().getRefundStatus());
    }

    @Test
    void shouldReturnFailedWhenRefundNotFound() {
        CrmebCompatResult<CrmebAdminRefundCompatController.CrmebRefundQueryRespVO> result =
                controller.query(null, null, "NOT_EXIST", null, null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("退款单不存在"));
    }

    @Test
    void shouldQueryRefundByPayRefundIdAndMapFailureStatus() {
        PayRefundDO refund = new PayRefundDO();
        refund.setId(15L);
        refund.setNo("rf_015");
        refund.setMerchantOrderId("ORDER_015");
        refund.setMerchantRefundId("MR_015");
        refund.setStatus(PayRefundStatusEnum.FAILURE.getStatus());
        refund.setPayPrice(500);
        refund.setRefundPrice(500);
        refund.setChannelErrorCode("SYSTEMERROR");
        refund.setChannelErrorMsg("refund failed");
        when(payRefundService.getRefund(15L)).thenReturn(refund);

        CrmebCompatResult<CrmebAdminRefundCompatController.CrmebRefundQueryRespVO> result =
                controller.query(15L, null, null, null, null);

        assertEquals(200, result.getCode());
        assertEquals(15L, result.getData().getPayRefundId());
        assertEquals("退款失败", result.getData().getStatusText());
        assertEquals(-1, result.getData().getRefundStatus());
        assertEquals("SYSTEMERROR", result.getData().getChannelErrorCode());
    }

}
