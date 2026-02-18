package com.zbkj.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.request.ServiceBookingVerifyRequest;
import com.zbkj.common.response.ServiceBookingCardCheckResponse;
import com.zbkj.common.response.ServiceBookingVerifyRecordResponse;
import com.zbkj.service.dao.BookingOrderDao;
import com.zbkj.service.dao.MemberCardDao;
import com.zbkj.service.dao.MemberCardUsageDao;
import com.zbkj.service.dao.TechnicianScheduleDao;
import com.zbkj.service.model.BookingOrder;
import com.zbkj.service.model.MemberCard;
import com.zbkj.service.model.MemberCardUsage;
import com.zbkj.service.model.TechnicianSchedule;
import com.zbkj.service.service.impl.payment.WeChatPayConfigSupport;
import com.zbkj.service.util.DistributedLockUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceBookingServiceImplTest {

    @Mock
    private BookingOrderDao bookingOrderDao;
    @Mock
    private TechnicianScheduleDao technicianScheduleDao;
    @Mock
    private MemberCardDao memberCardDao;
    @Mock
    private MemberCardUsageDao memberCardUsageDao;
    @Mock
    private DistributedLockUtil distributedLockUtil;
    @Mock
    private WeChatPayConfigSupport weChatPayConfigSupport;

    private ServiceBookingServiceImpl service;

    @BeforeEach
    void setUp() {
        initTableInfoCache();
        service = new ServiceBookingServiceImpl();
        ReflectionTestUtils.setField(service, "bookingOrderDao", bookingOrderDao);
        ReflectionTestUtils.setField(service, "technicianScheduleDao", technicianScheduleDao);
        ReflectionTestUtils.setField(service, "memberCardDao", memberCardDao);
        ReflectionTestUtils.setField(service, "memberCardUsageDao", memberCardUsageDao);
        ReflectionTestUtils.setField(service, "distributedLockUtil", distributedLockUtil);
        ReflectionTestUtils.setField(service, "weChatPayConfigSupport", weChatPayConfigSupport);

        lenient().when(distributedLockUtil.executeWithLock(anyString(), anyInt(), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Object> supplier = invocation.getArgument(2, Supplier.class);
                    return supplier.get();
                });
    }

    @Test
    void checkMemberCardShouldReturnNoMemberCardWhenOrderWithoutCard() {
        BookingOrder order = new BookingOrder();
        order.setOrderNo("BK001");
        order.setMemberCardId(0L);
        when(bookingOrderDao.selectOne(any())).thenReturn(order);

        ServiceBookingCardCheckResponse response = service.checkMemberCard("BK001", null, null);

        Assertions.assertTrue(response.getAvailable());
        Assertions.assertEquals("NO_MEMBER_CARD", response.getReasonCode());
        Assertions.assertEquals(BigDecimal.ZERO, response.getRequiredValue());
    }

    @Test
    void checkMemberCardShouldReturnInsufficientWhenTimesCardNotEnough() {
        BookingOrder order = new BookingOrder();
        order.setOrderNo("BK002");
        order.setUid(66);
        order.setMemberCardId(200L);

        MemberCard card = new MemberCard();
        card.setId(200L);
        card.setUid(66);
        card.setCardType(1);
        card.setStatus(1);
        card.setExpireTime((int) (System.currentTimeMillis() / 1000) + 3600);
        card.setRemainingValue(new BigDecimal("1"));

        when(bookingOrderDao.selectOne(any())).thenReturn(order);
        when(memberCardDao.selectById(200L)).thenReturn(card);

        ServiceBookingCardCheckResponse response = service.checkMemberCard("BK002", 2, null);

        Assertions.assertFalse(response.getAvailable());
        Assertions.assertEquals("INSUFFICIENT_BALANCE", response.getReasonCode());
        Assertions.assertEquals(new BigDecimal("2"), response.getRequiredValue());
    }

    @Test
    void listVerifyRecordsShouldMapUsageRows() {
        BookingOrder order = new BookingOrder();
        order.setId(88L);
        order.setOrderNo("BK003");
        when(bookingOrderDao.selectOne(any())).thenReturn(order);

        MemberCardUsage usage = new MemberCardUsage();
        usage.setId(1001L);
        usage.setUserCardId(700L);
        usage.setOrderId(88L);
        usage.setUsageType(2);
        usage.setUsedTimes(0);
        usage.setUsedAmount(new BigDecimal("39.90"));
        usage.setBeforeAmount(new BigDecimal("100"));
        usage.setAfterAmount(new BigDecimal("60.10"));
        usage.setStoreId(11);
        usage.setTechnicianId(22);
        usage.setCreatedAt(1739491200);
        when(memberCardUsageDao.selectList(any())).thenReturn(Arrays.asList(usage));

        List<ServiceBookingVerifyRecordResponse> records = service.listVerifyRecords("BK003");

        Assertions.assertEquals(1, records.size());
        ServiceBookingVerifyRecordResponse record = records.get(0);
        Assertions.assertEquals("BK003", record.getOrderNo());
        Assertions.assertEquals(1001L, record.getUsageId());
        Assertions.assertEquals(new BigDecimal("39.90"), record.getUsedAmount());
    }

    @Test
    void verifyShouldConsumeCardAndUpdateStatusWhenOrderPaid() {
        BookingOrder order = new BookingOrder();
        order.setId(301L);
        order.setOrderNo("BK004");
        order.setUid(99);
        order.setScheduleId(9);
        order.setSlotId("slot-9");
        order.setStatus(2);
        order.setCheckInCode("CI123456");
        order.setMemberCardId(501L);
        order.setStoreId(10);
        order.setTechnicianId(20);
        order.setActualPrice(new BigDecimal("88"));
        when(bookingOrderDao.selectOne(any())).thenReturn(order);

        TechnicianSchedule schedule = new TechnicianSchedule();
        schedule.setId(9);
        schedule.setTimeSlots(JSON.toJSONString(Arrays.asList(JSON.parseObject("{\"id\":\"slot-9\",\"status\":\"booked\",\"orderNo\":\"BK004\"}"))));
        when(technicianScheduleDao.selectById(9)).thenReturn(schedule);
        when(technicianScheduleDao.updateById(any())).thenReturn(1);

        MemberCard card = new MemberCard();
        card.setId(501L);
        card.setUid(99);
        card.setCardType(1);
        card.setRemainingValue(new BigDecimal("5"));
        card.setStatus(1);
        card.setExpireTime((int) (System.currentTimeMillis() / 1000) + 3600);
        when(memberCardDao.selectById(501L)).thenReturn(card);
        when(memberCardDao.update(any(), any())).thenReturn(1);
        when(memberCardUsageDao.insert(any())).thenReturn(1);
        when(bookingOrderDao.update(any(), any())).thenReturn(1);

        ServiceBookingVerifyRequest request = new ServiceBookingVerifyRequest();
        request.setOrderNo("BK004");
        request.setCheckInCode("CI123456");
        request.setUsageTimes(2);

        Boolean result = service.verify(request);

        Assertions.assertTrue(result);
        ArgumentCaptor<MemberCardUsage> usageCaptor = ArgumentCaptor.forClass(MemberCardUsage.class);
        verify(memberCardUsageDao).insert(usageCaptor.capture());
        Assertions.assertEquals(2, usageCaptor.getValue().getUsedTimes());
        Assertions.assertEquals(new BigDecimal("3"), usageCaptor.getValue().getAfterAmount());
        verify(bookingOrderDao).update(any(), any());
    }

    @Test
    void verifyShouldRejectWhenCheckInCodeMismatch() {
        BookingOrder order = new BookingOrder();
        order.setOrderNo("BK005");
        order.setScheduleId(11);
        order.setSlotId("slot-11");
        order.setStatus(2);
        order.setCheckInCode("CI000001");
        when(bookingOrderDao.selectOne(any())).thenReturn(order);

        ServiceBookingVerifyRequest request = new ServiceBookingVerifyRequest();
        request.setOrderNo("BK005");
        request.setCheckInCode("CI000002");

        CrmebException ex = Assertions.assertThrows(CrmebException.class, () -> service.verify(request));
        Assertions.assertTrue(ex.getMessage().contains("核销码不匹配"));
    }

    @Test
    void verifyShouldRejectWhenOrderNotPaid() {
        BookingOrder order = new BookingOrder();
        order.setOrderNo("BK006");
        order.setScheduleId(12);
        order.setSlotId("slot-12");
        order.setStatus(1);
        when(bookingOrderDao.selectOne(any())).thenReturn(order);

        ServiceBookingVerifyRequest request = new ServiceBookingVerifyRequest();
        request.setOrderNo("BK006");

        CrmebException ex = Assertions.assertThrows(CrmebException.class, () -> service.verify(request));
        Assertions.assertTrue(ex.getMessage().contains("当前订单状态不可核销"));
    }

    private void initTableInfoCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        safeInitTableInfo(assistant, BookingOrder.class);
        safeInitTableInfo(assistant, MemberCard.class);
        safeInitTableInfo(assistant, MemberCardUsage.class);
        safeInitTableInfo(assistant, TechnicianSchedule.class);
    }

    private void safeInitTableInfo(MapperBuilderAssistant assistant, Class<?> entityClass) {
        try {
            TableInfoHelper.getTableInfo(entityClass);
            if (TableInfoHelper.getTableInfo(entityClass) == null) {
                TableInfoHelper.initTableInfo(assistant, entityClass);
            }
        } catch (Exception ignore) {
            // Ignore duplicate init or mapper-less cases; we only need lambda metadata cache for unit tests.
        }
    }
}
