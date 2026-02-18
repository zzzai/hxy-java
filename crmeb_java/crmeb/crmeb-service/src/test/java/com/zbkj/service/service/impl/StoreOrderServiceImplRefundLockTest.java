package com.zbkj.service.service.impl;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.request.StoreOrderRefundRequest;
import com.zbkj.service.util.DistributedLockUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreOrderServiceImplRefundLockTest {

    @Mock
    private DistributedLockUtil distributedLockUtil;

    private StoreOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StoreOrderServiceImpl();
        ReflectionTestUtils.setField(service, "distributedLockUtil", distributedLockUtil);
    }

    @Test
    void refundShouldUseOrderNoAsDistributedLockKey() {
        StoreOrderRefundRequest request = new StoreOrderRefundRequest();
        request.setOrderNo("order63147177138520007029302");
        when(distributedLockUtil.executeWithLock(eq("pay:refund:lock:order63147177138520007029302"), eq(20), any(Supplier.class)))
                .thenReturn(Boolean.TRUE);

        boolean result = service.refund(request);

        Assertions.assertTrue(result);
        verify(distributedLockUtil).executeWithLock(eq("pay:refund:lock:order63147177138520007029302"), eq(20), any(Supplier.class));
    }

    @Test
    void refundShouldRejectBlankOrderNoBeforeLock() {
        StoreOrderRefundRequest request = new StoreOrderRefundRequest();
        request.setOrderNo("   ");

        CrmebException exception = Assertions.assertThrows(CrmebException.class, () -> service.refund(request));

        Assertions.assertEquals("订单编号不能为空", exception.getMessage());
        verify(distributedLockUtil, never()).executeWithLock(any(String.class), any(Integer.class), any(Supplier.class));
    }
}
