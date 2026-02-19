package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.common.vo.MyRecord;
import com.zbkj.common.vo.WeChatPayChannelConfig;
import com.zbkj.service.service.StoreOrderService;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.WechatNewService;
import com.zbkj.service.service.impl.payment.WeChatPayConfigSupport;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTaskServiceImplRefundResidualCompensateTest {

    @Mock
    private StoreOrderService storeOrderService;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private WechatNewService wechatNewService;
    @Mock
    private WeChatPayConfigSupport weChatPayConfigSupport;
    @Mock
    private RedisUtil redisUtil;

    private OrderTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        initStoreOrderTableInfo();
        service = new OrderTaskServiceImpl();
        ReflectionTestUtils.setField(service, "storeOrderService", storeOrderService);
        ReflectionTestUtils.setField(service, "systemConfigService", systemConfigService);
        ReflectionTestUtils.setField(service, "wechatNewService", wechatNewService);
        ReflectionTestUtils.setField(service, "weChatPayConfigSupport", weChatPayConfigSupport);
        ReflectionTestUtils.setField(service, "redisUtil", redisUtil);
    }

    @Test
    void refundTimeoutCompensateShouldConvergeResidualRefundOnSuccess() {
        StoreOrder order = buildResidualRefundOrder();
        WeChatPayChannelConfig config = new WeChatPayChannelConfig();
        when(systemConfigService.getValueByKey(any())).thenReturn(null);
        when(storeOrderService.list(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList(), Collections.emptyList(), Collections.singletonList(order));
        when(weChatPayConfigSupport.resolveByChannel(eq(order.getIsChannel()), eq(order.getStoreId()))).thenReturn(config);
        when(wechatNewService.payRefundQuery(eq(order.getOrderId()), eq(config))).thenReturn(new MyRecord().set("status", "SUCCESS"));
        when(storeOrderService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

        ReflectionTestUtils.invokeMethod(service, "refundTimeoutCompensate");

        verify(storeOrderService).update(any(LambdaUpdateWrapper.class));
        verify(redisUtil).lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER, order.getId());
    }

    @Test
    void refundTimeoutCompensateShouldKeepResidualRefundWhenNotSuccess() {
        StoreOrder order = buildResidualRefundOrder();
        WeChatPayChannelConfig config = new WeChatPayChannelConfig();
        when(systemConfigService.getValueByKey(any())).thenReturn(null);
        when(storeOrderService.list(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList(), Collections.emptyList(), Collections.singletonList(order));
        when(weChatPayConfigSupport.resolveByChannel(eq(order.getIsChannel()), eq(order.getStoreId()))).thenReturn(config);
        when(wechatNewService.payRefundQuery(eq(order.getOrderId()), eq(config))).thenReturn(new MyRecord().set("status", "PROCESSING"));

        ReflectionTestUtils.invokeMethod(service, "refundTimeoutCompensate");

        verify(storeOrderService, never()).update(any(LambdaUpdateWrapper.class));
        verify(redisUtil, never()).lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER, order.getId());
    }

    private StoreOrder buildResidualRefundOrder() {
        StoreOrder order = new StoreOrder();
        order.setId(101);
        order.setOrderId("order_residual_refund_101");
        order.setOutTradeNo("wx_residual_refund_101");
        order.setRefundStatus(0);
        order.setRefundPrice(new BigDecimal("0.01"));
        order.setPaid(true);
        order.setPayType(Constants.PAY_TYPE_WE_CHAT);
        order.setIsChannel(1);
        order.setStoreId(0);
        return order;
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
