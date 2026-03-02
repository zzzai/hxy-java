package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionDO;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionConfigMapper;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionMapper;
import com.hxy.module.booking.enums.CommissionStatusEnum;
import com.hxy.module.booking.enums.CommissionTypeEnum;
import com.hxy.module.booking.service.BookingOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static com.hxy.module.booking.enums.ErrorCodeConstants.COMMISSION_REVERSAL_IDEMPOTENT_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicianCommissionServiceImplCancelCommissionTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TechnicianCommissionServiceImpl service;

    @Mock
    private TechnicianCommissionMapper commissionMapper;
    @Mock
    private TechnicianCommissionConfigMapper commissionConfigMapper;
    @Mock
    private BookingOrderService bookingOrderService;

    @Test
    void shouldCancelPositivePendingCommission() {
        Long orderId = 1001L;
        TechnicianCommissionDO pending = TechnicianCommissionDO.builder()
                .id(11L)
                .orderId(orderId)
                .technicianId(10L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(10000)
                .commissionAmount(1500)
                .status(CommissionStatusEnum.PENDING.getStatus())
                .build();
        when(commissionMapper.selectListByOrderId(orderId)).thenReturn(Collections.singletonList(pending));

        service.cancelCommission(orderId);

        ArgumentCaptor<TechnicianCommissionDO> captor = ArgumentCaptor.forClass(TechnicianCommissionDO.class);
        verify(commissionMapper).updateById(captor.capture());
        assertEquals(11L, captor.getValue().getId());
        assertEquals(CommissionStatusEnum.CANCELLED.getStatus(), captor.getValue().getStatus());
        verify(commissionMapper, never()).insert(any(TechnicianCommissionDO.class));
    }

    @Test
    void shouldCreateReversalForSettledCommission() {
        Long orderId = 1002L;
        TechnicianCommissionDO settled = TechnicianCommissionDO.builder()
                .id(21L)
                .orderId(orderId)
                .userId(2001L)
                .storeId(3001L)
                .technicianId(20L)
                .commissionType(CommissionTypeEnum.DESIGNATED.getType())
                .baseAmount(12000)
                .commissionRate(new BigDecimal("0.20"))
                .commissionAmount(2400)
                .status(CommissionStatusEnum.SETTLED.getStatus())
                .build();
        when(commissionMapper.selectListByOrderId(orderId)).thenReturn(Collections.singletonList(settled));

        service.cancelCommission(orderId);

        ArgumentCaptor<TechnicianCommissionDO> captor = ArgumentCaptor.forClass(TechnicianCommissionDO.class);
        verify(commissionMapper).insert(captor.capture());
        TechnicianCommissionDO reversal = captor.getValue();
        assertEquals(orderId, reversal.getOrderId());
        assertEquals(settled.getTechnicianId(), reversal.getTechnicianId());
        assertEquals(settled.getUserId(), reversal.getUserId());
        assertEquals(settled.getStoreId(), reversal.getStoreId());
        assertEquals(settled.getCommissionType(), reversal.getCommissionType());
        assertEquals(settled.getCommissionRate(), reversal.getCommissionRate());
        assertEquals(-12000, reversal.getBaseAmount());
        assertEquals(-2400, reversal.getCommissionAmount());
        assertEquals(CommissionStatusEnum.PENDING.getStatus(), reversal.getStatus());
        assertNull(reversal.getSettlementId());
        assertNull(reversal.getSettlementTime());
        assertEquals("ORDER_CANCEL_REVERSAL", reversal.getBizType());
        assertEquals("21", reversal.getBizNo());
        assertEquals(settled.getTechnicianId(), reversal.getStaffId());
    }

    @Test
    void shouldBeIdempotentWhenSettledAlreadyHasReversal() {
        Long orderId = 1003L;
        TechnicianCommissionDO settled = TechnicianCommissionDO.builder()
                .id(31L)
                .orderId(orderId)
                .userId(2002L)
                .storeId(3002L)
                .technicianId(30L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(8000)
                .commissionRate(new BigDecimal("0.15"))
                .commissionAmount(1200)
                .status(CommissionStatusEnum.SETTLED.getStatus())
                .build();
        TechnicianCommissionDO reversal = TechnicianCommissionDO.builder()
                .id(32L)
                .orderId(orderId)
                .userId(2002L)
                .storeId(3002L)
                .technicianId(30L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(-8000)
                .commissionRate(new BigDecimal("0.15"))
                .commissionAmount(-1200)
                .bizType("ORDER_CANCEL_REVERSAL")
                .bizNo("31")
                .staffId(30L)
                .status(CommissionStatusEnum.PENDING.getStatus())
                .build();
        when(commissionMapper.selectListByOrderId(orderId))
                .thenReturn(Collections.singletonList(settled))
                .thenReturn(Arrays.asList(settled, reversal));

        service.cancelCommission(orderId);
        service.cancelCommission(orderId);

        verify(commissionMapper, times(1)).insert(any(TechnicianCommissionDO.class));
    }

    @Test
    void shouldNotCancelPendingReversalCommission() {
        Long orderId = 1004L;
        TechnicianCommissionDO reversal = TechnicianCommissionDO.builder()
                .id(41L)
                .orderId(orderId)
                .technicianId(40L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(-10000)
                .commissionAmount(-1500)
                .status(CommissionStatusEnum.PENDING.getStatus())
                .build();
        when(commissionMapper.selectListByOrderId(orderId)).thenReturn(Collections.singletonList(reversal));

        service.cancelCommission(orderId);

        verify(commissionMapper, never()).updateById(any(TechnicianCommissionDO.class));
    }

    @Test
    void shouldCreateReversalWhenExistingReversalIsCancelled() {
        Long orderId = 1005L;
        TechnicianCommissionDO settled = TechnicianCommissionDO.builder()
                .id(51L)
                .orderId(orderId)
                .userId(2005L)
                .storeId(3005L)
                .technicianId(50L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(10000)
                .commissionRate(new BigDecimal("0.10"))
                .commissionAmount(1000)
                .status(CommissionStatusEnum.SETTLED.getStatus())
                .build();
        TechnicianCommissionDO cancelledReversal = TechnicianCommissionDO.builder()
                .id(52L)
                .orderId(orderId)
                .userId(2005L)
                .storeId(3005L)
                .technicianId(50L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(-10000)
                .commissionRate(new BigDecimal("0.10"))
                .commissionAmount(-1000)
                .bizType("ORDER_CANCEL_REVERSAL")
                .bizNo("51")
                .staffId(50L)
                .status(CommissionStatusEnum.CANCELLED.getStatus())
                .build();
        when(commissionMapper.selectListByOrderId(orderId))
                .thenReturn(Arrays.asList(settled, cancelledReversal));

        service.cancelCommission(orderId);

        verify(commissionMapper, times(1)).reactivateCancelledReversalById(
                eq(52L), eq(-10000), eq(new BigDecimal("0.10")), eq(-1000),
                eq("ORDER_CANCEL_REVERSAL"), eq("51"), eq(50L));
        verify(commissionMapper, never()).insert(any(TechnicianCommissionDO.class));
    }

    @Test
    void shouldThrowWhenReversalIdempotentKeyConflictDifferentAmount() {
        Long orderId = 1006L;
        TechnicianCommissionDO settled = TechnicianCommissionDO.builder()
                .id(61L)
                .orderId(orderId)
                .userId(2006L)
                .storeId(3006L)
                .technicianId(60L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(12000)
                .commissionRate(new BigDecimal("0.10"))
                .commissionAmount(1200)
                .status(CommissionStatusEnum.SETTLED.getStatus())
                .build();
        TechnicianCommissionDO conflicted = TechnicianCommissionDO.builder()
                .id(62L)
                .orderId(orderId)
                .userId(2006L)
                .storeId(3006L)
                .technicianId(60L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(-11000)
                .commissionRate(new BigDecimal("0.10"))
                .commissionAmount(-1100)
                .bizType("ORDER_CANCEL_REVERSAL")
                .bizNo("61")
                .staffId(60L)
                .status(CommissionStatusEnum.PENDING.getStatus())
                .build();
        when(commissionMapper.selectListByOrderId(orderId)).thenReturn(Arrays.asList(settled, conflicted));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.cancelCommission(orderId));

        assertEquals(COMMISSION_REVERSAL_IDEMPOTENT_CONFLICT.getCode(), ex.getCode());
        verify(commissionMapper, never()).insert(any(TechnicianCommissionDO.class));
        verify(commissionMapper, never()).reactivateCancelledReversalById(any(), any(), any(), any(), any(), any(), any());
    }
}
