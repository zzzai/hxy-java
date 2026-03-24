package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionConfigSaveReqVO;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionRespVO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionConfigDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionDO;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionConfigMapper;
import com.hxy.module.booking.service.TechnicianCommissionService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicianCommissionControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TechnicianCommissionController controller;

    @Mock
    private TechnicianCommissionService commissionService;
    @Mock
    private TechnicianCommissionConfigMapper commissionConfigMapper;

    @Test
    void shouldGetCommissionListByTechnician() {
        TechnicianCommissionDO record = new TechnicianCommissionDO();
        record.setId(101L);
        record.setTechnicianId(9001L);
        record.setOrderId(8001L);
        record.setStoreId(7001L);
        record.setCommissionType(1);
        record.setCommissionRate(new BigDecimal("0.15"));
        record.setCommissionAmount(1800);
        record.setStatus(0);
        record.setSourceBizNo("BOOKING_COMMISSION_ACCRUAL");
        record.setCreateTime(LocalDateTime.now());
        when(commissionService.getCommissionListByTechnician(9001L)).thenReturn(List.of(record));

        CommonResult<List<TechnicianCommissionRespVO>> result = controller.getCommissionListByTechnician(9001L);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals(101L, result.getData().get(0).getId());
        assertEquals(9001L, result.getData().get(0).getTechnicianId());
        assertEquals(1800, result.getData().get(0).getCommissionAmount());
        verify(commissionService).getCommissionListByTechnician(9001L);
    }

    @Test
    void shouldReturnPendingCommissionAmount() {
        when(commissionService.getPendingCommissionAmount(9002L)).thenReturn(3600);

        CommonResult<Integer> result = controller.getPendingCommissionAmount(9002L);

        assertTrue(result.isSuccess());
        assertEquals(3600, result.getData());
        verify(commissionService).getPendingCommissionAmount(9002L);
    }

    @Test
    void shouldWrapSettleAndBatchSettleAsTrue() {
        CommonResult<Boolean> settleResult = controller.settleCommission(111L);
        CommonResult<Boolean> batchResult = controller.batchSettle(9003L);

        assertTrue(settleResult.isSuccess());
        assertEquals(Boolean.TRUE, settleResult.getData());
        assertTrue(batchResult.isSuccess());
        assertEquals(Boolean.TRUE, batchResult.getData());
        verify(commissionService).settleCommission(111L);
        verify(commissionService).batchSettleByTechnician(9003L);
    }

    @Test
    void shouldSaveAndDeleteConfigAsTrue() {
        TechnicianCommissionConfigSaveReqVO reqVO = new TechnicianCommissionConfigSaveReqVO();
        reqVO.setStoreId(7002L);
        reqVO.setCommissionType(2);
        reqVO.setRate(new BigDecimal("0.20"));
        reqVO.setFixedAmount(500);

        CommonResult<Boolean> saveResult = controller.saveConfig(reqVO);
        CommonResult<Boolean> deleteResult = controller.deleteConfig(501L);

        assertTrue(saveResult.isSuccess());
        assertEquals(Boolean.TRUE, saveResult.getData());
        assertTrue(deleteResult.isSuccess());
        assertEquals(Boolean.TRUE, deleteResult.getData());
        verify(commissionConfigMapper).insert(any(TechnicianCommissionConfigDO.class));
        verify(commissionConfigMapper).deleteById(501L);
    }
}
