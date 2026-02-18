package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.service.service.StoreOrderService;
import com.zbkj.service.service.StoreOrderStatusService;
import com.zbkj.service.service.impl.payment.OrderRefundStateMachine;
import com.zbkj.service.util.DistributedLockUtil;
import org.apache.ibatis.builder.MapperBuilderAssistant;
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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CallbackServiceImplRefundFallbackTest {

    @Mock
    private StoreOrderService storeOrderService;
    @Mock
    private RedisUtil redisUtil;
    @Mock
    private StoreOrderStatusService storeOrderStatusService;
    @Mock
    private DistributedLockUtil distributedLockUtil;

    private CallbackServiceImpl service;

    @BeforeEach
    void setUp() {
        initStoreOrderTableInfo();
        service = new CallbackServiceImpl();
        ReflectionTestUtils.setField(service, "storeOrderService", storeOrderService);
        ReflectionTestUtils.setField(service, "redisUtil", redisUtil);
        ReflectionTestUtils.setField(service, "storeOrderStatusService", storeOrderStatusService);
        ReflectionTestUtils.setField(service, "distributedLockUtil", distributedLockUtil);
        lenient().when(distributedLockUtil.executeWithLock(any(), any(Integer.class), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<Boolean> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
    }

    @Test
    void resolveRefundTargetOrderShouldFallbackToOutTradeNoWhenOrderIdNotFound() {
        StoreOrder expected = new StoreOrder();
        expected.setId(88);
        expected.setOutTradeNo("wxNo89809177138520395483906");
        when(storeOrderService.getByOderId("wxNo89809177138520395483906")).thenReturn(null);
        when(storeOrderService.getOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(expected);

        StoreOrder actual = ReflectionTestUtils.invokeMethod(service, "resolveRefundTargetOrder", "wxNo89809177138520395483906");

        Assertions.assertSame(expected, actual);
        verify(storeOrderService).getByOderId("wxNo89809177138520395483906");
        verify(storeOrderService).getOne(any(LambdaQueryWrapper.class), eq(false));
    }

    @Test
    void settleRefundSuccessShouldPushAfterRefundTaskWhenFallbackMatched() {
        StoreOrder target = new StoreOrder();
        target.setId(48);
        target.setOrderId("order63147177138520007029302");
        target.setOutTradeNo("wxNo89809177138520395483906");
        target.setRefundStatus(OrderRefundStateMachine.REFUND_STATUS_APPLYING);

        when(storeOrderService.getByOderId("wxNo89809177138520395483906")).thenReturn(null);
        when(storeOrderService.getOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(target);
        when(storeOrderService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);
        when(storeOrderStatusService.count(any(LambdaQueryWrapper.class))).thenReturn(0);

        ReflectionTestUtils.invokeMethod(service, "settleRefundSuccess", "wxNo89809177138520395483906", "{\"event_type\":\"REFUND.SUCCESS\"}");

        verify(storeOrderService).update(any(LambdaUpdateWrapper.class));
        verify(redisUtil).lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER, target.getId());
    }

    @Test
    void settleRefundSuccessShouldSkipWhenOrderAlreadyFinalSuccess() {
        StoreOrder target = new StoreOrder();
        target.setId(49);
        target.setOrderId("order_test_final");
        target.setRefundStatus(OrderRefundStateMachine.REFUND_STATUS_SUCCESS);
        when(storeOrderService.getByOderId("order_test_final")).thenReturn(target);
        when(storeOrderStatusService.count(any(LambdaQueryWrapper.class))).thenReturn(0);

        ReflectionTestUtils.invokeMethod(service, "settleRefundSuccess", "order_test_final", "{\"event_type\":\"REFUND.SUCCESS\"}");

        verify(storeOrderService, never()).update(any(LambdaUpdateWrapper.class));
        verify(redisUtil, never()).lPush(any(String.class), any());
    }

    @Test
    void settleRefundSuccessShouldSkipWhenCallbackAlreadyConsumed() {
        StoreOrder target = new StoreOrder();
        target.setId(50);
        target.setOrderId("order_dedup_hit");
        target.setRefundStatus(OrderRefundStateMachine.REFUND_STATUS_APPLYING);
        when(storeOrderService.getByOderId("order_dedup_hit")).thenReturn(target);
        when(storeOrderStatusService.count(any(LambdaQueryWrapper.class))).thenReturn(1);

        ReflectionTestUtils.invokeMethod(service, "settleRefundSuccess", "order_dedup_hit", "{\"event_type\":\"REFUND.SUCCESS\"}");

        verify(storeOrderService, never()).update(any(LambdaUpdateWrapper.class));
        verify(redisUtil, never()).lPush(any(String.class), any());
    }

    private void initStoreOrderTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        try {
            TableInfoHelper.getTableInfo(StoreOrder.class);
            if (TableInfoHelper.getTableInfo(StoreOrder.class) == null) {
                TableInfoHelper.initTableInfo(assistant, StoreOrder.class);
            }
        } catch (Exception ignore) {
            // Ignore duplicate init in repeated runs; tests only need lambda metadata cache.
        }
    }
}
