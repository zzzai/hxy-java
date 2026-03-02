package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionConfigDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionDO;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionConfigMapper;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionMapper;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import com.hxy.module.booking.enums.CommissionStatusEnum;
import com.hxy.module.booking.enums.CommissionTypeEnum;
import com.hxy.module.booking.enums.DispatchModeEnum;
import com.hxy.module.booking.service.BookingOrderService;
import org.mybatis.spring.annotation.MapperScan;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Import(TechnicianCommissionServiceImpl.class)
@MapperScan("com.hxy.module.booking.dal.mysql")
public class TechnicianCommissionServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TechnicianCommissionServiceImpl commissionService;

    @Resource
    private TechnicianCommissionMapper commissionMapper;

    @Resource
    private TechnicianCommissionConfigMapper commissionConfigMapper;

    @MockBean
    private BookingOrderService bookingOrderService;

    @Test
    public void testCalculateCommission_baseType() {
        // 准备：排钟模式的已完成订单
        Long orderId = 1L;
        BookingOrderDO order = buildOrder(orderId, DispatchModeEnum.AUTO_ASSIGN.getMode(), 0);
        order.setStatus(BookingOrderStatusEnum.COMPLETED.getStatus());
        order.setPayPrice(10000);
        when(bookingOrderService.getOrder(eq(orderId))).thenReturn(order);

        // 调用
        commissionService.calculateCommission(orderId);

        // 断言
        List<TechnicianCommissionDO> list = commissionMapper.selectListByOrderId(orderId);
        assertEquals(1, list.size());
        TechnicianCommissionDO commission = list.get(0);
        assertEquals(CommissionTypeEnum.BASE.getType(), commission.getCommissionType());
        assertEquals(10000, commission.getBaseAmount());
        // 默认 15% = 1500
        assertEquals(1500, commission.getCommissionAmount());
        assertEquals(CommissionStatusEnum.PENDING.getStatus(), commission.getStatus());
    }

    @Test
    public void testCalculateCommission_designatedType() {
        // 准备：点钟模式
        Long orderId = 2L;
        BookingOrderDO order = buildOrder(orderId, DispatchModeEnum.DESIGNATED.getMode(), 0);
        order.setStatus(BookingOrderStatusEnum.COMPLETED.getStatus());
        order.setPayPrice(10000);
        when(bookingOrderService.getOrder(eq(orderId))).thenReturn(order);

        commissionService.calculateCommission(orderId);

        List<TechnicianCommissionDO> list = commissionMapper.selectListByOrderId(orderId);
        assertEquals(1, list.size());
        assertEquals(CommissionTypeEnum.DESIGNATED.getType(), list.get(0).getCommissionType());
        // 默认 20% = 2000
        assertEquals(2000, list.get(0).getCommissionAmount());
    }

    @Test
    public void testCalculateCommission_extendAddon() {
        // 准备：加钟订单
        Long orderId = 3L;
        BookingOrderDO order = buildOrder(orderId, DispatchModeEnum.DESIGNATED.getMode(), 1);
        order.setAddonType(1); // EXTEND
        order.setStatus(BookingOrderStatusEnum.COMPLETED.getStatus());
        order.setPayPrice(5000);
        when(bookingOrderService.getOrder(eq(orderId))).thenReturn(order);

        commissionService.calculateCommission(orderId);

        List<TechnicianCommissionDO> list = commissionMapper.selectListByOrderId(orderId);
        assertEquals(1, list.size());
        assertEquals(CommissionTypeEnum.EXTEND.getType(), list.get(0).getCommissionType());
        // 默认 20% = 1000
        assertEquals(1000, list.get(0).getCommissionAmount());
    }

    @Test
    public void testCalculateCommission_storeConfigOverride() {
        // 准备：门店配置覆盖默认比例
        Long orderId = 4L;
        Long storeId = 10L;
        BookingOrderDO order = buildOrder(orderId, DispatchModeEnum.DESIGNATED.getMode(), 0);
        order.setStoreId(storeId);
        order.setStatus(BookingOrderStatusEnum.COMPLETED.getStatus());
        order.setPayPrice(10000);
        when(bookingOrderService.getOrder(eq(orderId))).thenReturn(order);

        // 插入门店佣金配置：点钟 30%
        TechnicianCommissionConfigDO config = TechnicianCommissionConfigDO.builder()
                .storeId(storeId)
                .commissionType(CommissionTypeEnum.DESIGNATED.getType())
                .rate(new BigDecimal("0.30"))
                .fixedAmount(0)
                .build();
        commissionConfigMapper.insert(config);

        commissionService.calculateCommission(orderId);

        List<TechnicianCommissionDO> list = commissionMapper.selectListByOrderId(orderId);
        assertEquals(1, list.size());
        // 门店配置 30% = 3000
        assertEquals(3000, list.get(0).getCommissionAmount());
    }

    @Test
    public void testCalculateCommission_duplicatePrevention() {
        // 准备
        Long orderId = 5L;
        BookingOrderDO order = buildOrder(orderId, DispatchModeEnum.AUTO_ASSIGN.getMode(), 0);
        order.setStatus(BookingOrderStatusEnum.COMPLETED.getStatus());
        order.setPayPrice(10000);
        when(bookingOrderService.getOrder(eq(orderId))).thenReturn(order);

        // 第一次计算
        commissionService.calculateCommission(orderId);
        // 第二次计算（应跳过）
        commissionService.calculateCommission(orderId);

        List<TechnicianCommissionDO> list = commissionMapper.selectListByOrderId(orderId);
        assertEquals(1, list.size());
    }

    @Test
    public void testCalculateCommission_orderNotCompleted() {
        Long orderId = 6L;
        BookingOrderDO order = buildOrder(orderId, DispatchModeEnum.AUTO_ASSIGN.getMode(), 0);
        order.setStatus(BookingOrderStatusEnum.PAID.getStatus());
        when(bookingOrderService.getOrder(eq(orderId))).thenReturn(order);

        commissionService.calculateCommission(orderId);

        List<TechnicianCommissionDO> list = commissionMapper.selectListByOrderId(orderId);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testCancelCommission() {
        // 准备：插入待结算佣金
        Long orderId = 7L;
        TechnicianCommissionDO commission = TechnicianCommissionDO.builder()
                .technicianId(1L).orderId(orderId).userId(1L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(10000).commissionRate(new BigDecimal("0.15"))
                .commissionAmount(1500)
                .status(CommissionStatusEnum.PENDING.getStatus())
                .build();
        commissionMapper.insert(commission);

        commissionService.cancelCommission(orderId);

        TechnicianCommissionDO updated = commissionMapper.selectById(commission.getId());
        assertEquals(CommissionStatusEnum.CANCELLED.getStatus(), updated.getStatus());
    }

    @Test
    public void testCancelCommission_shouldCreateReversalWhenSettled() {
        Long orderId = 701L;
        TechnicianCommissionDO settled = TechnicianCommissionDO.builder()
                .technicianId(11L).orderId(orderId).userId(1001L).storeId(88L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(12000).commissionRate(new BigDecimal("0.15"))
                .commissionAmount(1800)
                .status(CommissionStatusEnum.SETTLED.getStatus())
                .build();
        commissionMapper.insert(settled);

        commissionService.cancelCommission(orderId);

        List<TechnicianCommissionDO> rows = commissionMapper.selectListByOrderId(orderId);
        assertEquals(2, rows.size());
        TechnicianCommissionDO reversal = rows.stream()
                .filter(row -> !row.getId().equals(settled.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(CommissionStatusEnum.PENDING.getStatus(), reversal.getStatus());
        assertEquals(-12000, reversal.getBaseAmount());
        assertEquals(-1800, reversal.getCommissionAmount());
        assertEquals(settled.getCommissionType(), reversal.getCommissionType());
        assertEquals(0, settled.getCommissionRate().compareTo(reversal.getCommissionRate()));
        assertEquals(settled.getTechnicianId(), reversal.getTechnicianId());
        assertEquals(settled.getStoreId(), reversal.getStoreId());
        assertEquals("ORDER_CANCEL_REVERSAL", reversal.getBizType());
        assertEquals(String.valueOf(settled.getId()), reversal.getBizNo());
        assertEquals(settled.getTechnicianId(), reversal.getStaffId());
        assertNull(reversal.getSettlementId());
        assertNull(reversal.getSettlementTime());
    }

    @Test
    public void testCancelCommission_shouldBeIdempotentForSettledReversal() {
        Long orderId = 702L;
        TechnicianCommissionDO settled = TechnicianCommissionDO.builder()
                .technicianId(12L).orderId(orderId).userId(1002L).storeId(89L)
                .commissionType(CommissionTypeEnum.DESIGNATED.getType())
                .baseAmount(9000).commissionRate(new BigDecimal("0.20"))
                .commissionAmount(1800)
                .status(CommissionStatusEnum.SETTLED.getStatus())
                .build();
        commissionMapper.insert(settled);

        commissionService.cancelCommission(orderId);
        commissionService.cancelCommission(orderId);

        List<TechnicianCommissionDO> rows = commissionMapper.selectListByOrderId(orderId);
        long reversalCount = rows.stream().filter(row -> row.getCommissionAmount() != null
                && row.getCommissionAmount() < 0).count();
        assertEquals(1, reversalCount);
    }

    @Test
    public void testCancelCommission_shouldReactivateCancelledReversalWithSameBizKey() {
        Long orderId = 703L;
        TechnicianCommissionDO settled = TechnicianCommissionDO.builder()
                .technicianId(13L).orderId(orderId).userId(1003L).storeId(90L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(11000).commissionRate(new BigDecimal("0.10"))
                .commissionAmount(1100)
                .status(CommissionStatusEnum.SETTLED.getStatus())
                .build();
        commissionMapper.insert(settled);
        TechnicianCommissionDO cancelledReversal = TechnicianCommissionDO.builder()
                .technicianId(13L).orderId(orderId).userId(1003L).storeId(90L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(-11000).commissionRate(new BigDecimal("0.10"))
                .commissionAmount(-1100)
                .status(CommissionStatusEnum.CANCELLED.getStatus())
                .bizType("ORDER_CANCEL_REVERSAL")
                .bizNo(String.valueOf(settled.getId()))
                .staffId(13L)
                .settlementId(999L)
                .build();
        commissionMapper.insert(cancelledReversal);

        commissionService.cancelCommission(orderId);

        List<TechnicianCommissionDO> rows = commissionMapper.selectListByOrderId(orderId);
        assertEquals(2, rows.size());
        TechnicianCommissionDO updatedReversal = commissionMapper.selectById(cancelledReversal.getId());
        assertEquals(CommissionStatusEnum.PENDING.getStatus(), updatedReversal.getStatus());
        assertEquals("ORDER_CANCEL_REVERSAL", updatedReversal.getBizType());
        assertEquals(String.valueOf(settled.getId()), updatedReversal.getBizNo());
        assertEquals(13L, updatedReversal.getStaffId());
        assertNull(updatedReversal.getSettlementId());
        assertNull(updatedReversal.getSettlementTime());
    }

    @Test
    public void testSettleCommission() {
        TechnicianCommissionDO commission = TechnicianCommissionDO.builder()
                .technicianId(1L).orderId(8L).userId(1L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(10000).commissionRate(new BigDecimal("0.15"))
                .commissionAmount(1500)
                .status(CommissionStatusEnum.PENDING.getStatus())
                .build();
        commissionMapper.insert(commission);

        commissionService.settleCommission(commission.getId());

        TechnicianCommissionDO updated = commissionMapper.selectById(commission.getId());
        assertEquals(CommissionStatusEnum.SETTLED.getStatus(), updated.getStatus());
        assertNotNull(updated.getSettlementTime());
    }

    @Test
    public void testGetPendingCommissionAmount() {
        Long technicianId = 100L;
        // 插入两条待结算
        commissionMapper.insert(TechnicianCommissionDO.builder()
                .technicianId(technicianId).orderId(9L).userId(1L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(10000).commissionRate(new BigDecimal("0.15"))
                .commissionAmount(1500)
                .status(CommissionStatusEnum.PENDING.getStatus()).build());
        commissionMapper.insert(TechnicianCommissionDO.builder()
                .technicianId(technicianId).orderId(10L).userId(2L)
                .commissionType(CommissionTypeEnum.DESIGNATED.getType())
                .baseAmount(8000).commissionRate(new BigDecimal("0.20"))
                .commissionAmount(1600)
                .status(CommissionStatusEnum.PENDING.getStatus()).build());
        // 插入一条已结算（不应计入）
        commissionMapper.insert(TechnicianCommissionDO.builder()
                .technicianId(technicianId).orderId(11L).userId(3L)
                .commissionType(CommissionTypeEnum.BASE.getType())
                .baseAmount(5000).commissionRate(new BigDecimal("0.15"))
                .commissionAmount(750)
                .status(CommissionStatusEnum.SETTLED.getStatus()).build());

        int amount = commissionService.getPendingCommissionAmount(technicianId);
        assertEquals(3100, amount);
    }

    private BookingOrderDO buildOrder(Long orderId, Integer dispatchMode, Integer isAddon) {
        BookingOrderDO order = new BookingOrderDO();
        order.setId(orderId);
        order.setOrderNo("BK" + orderId);
        order.setUserId(1L);
        order.setStoreId(1L);
        order.setTechnicianId(1L);
        order.setTimeSlotId(100L);
        order.setSpuId(200L);
        order.setSkuId(201L);
        order.setBookingDate(LocalDate.now());
        order.setBookingStartTime(LocalTime.of(9, 0));
        order.setBookingEndTime(LocalTime.of(10, 0));
        order.setDuration(60);
        order.setOriginalPrice(10000);
        order.setDiscountPrice(0);
        order.setPayPrice(10000);
        order.setIsOffpeak(false);
        order.setDispatchMode(dispatchMode);
        order.setIsAddon(isAddon);
        return order;
    }

}
