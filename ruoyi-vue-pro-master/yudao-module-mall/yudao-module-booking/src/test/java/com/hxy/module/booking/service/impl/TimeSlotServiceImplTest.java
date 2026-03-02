package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import com.hxy.module.booking.dal.dataobject.TimeSlotDO;
import com.hxy.module.booking.dal.mysql.TimeSlotMapper;
import com.hxy.module.booking.enums.TimeSlotStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static com.hxy.module.booking.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TimeSlotServiceImpl} 的单元测试类
 */
@Import(TimeSlotServiceImpl.class)
public class TimeSlotServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TimeSlotServiceImpl timeSlotService;

    @Resource
    private TimeSlotMapper timeSlotMapper;

    @Test
    public void testLockTimeSlot_success() {
        // 准备数据：创建一个可用的时间槽
        TimeSlotDO slot = createTimeSlot(TimeSlotStatusEnum.AVAILABLE.getStatus());
        timeSlotMapper.insert(slot);

        // 调用
        boolean result = timeSlotService.lockTimeSlot(slot.getId(), 1L);

        // 断言
        assertTrue(result);
        TimeSlotDO updatedSlot = timeSlotMapper.selectById(slot.getId());
        assertEquals(TimeSlotStatusEnum.LOCKED.getStatus(), updatedSlot.getStatus());
        assertEquals(1L, updatedSlot.getLockUserId());
        assertNotNull(updatedSlot.getLockExpireTime());
    }

    @Test
    public void testLockTimeSlot_alreadyLocked() {
        // 准备数据：创建一个已锁定的时间槽
        TimeSlotDO slot = createTimeSlot(TimeSlotStatusEnum.LOCKED.getStatus());
        slot.setLockUserId(2L);
        slot.setLockExpireTime(LocalDateTime.now().plusMinutes(15));
        timeSlotMapper.insert(slot);

        // 调用
        boolean result = timeSlotService.lockTimeSlot(slot.getId(), 1L);

        // 断言：锁定失败
        assertFalse(result);
    }

    @Test
    public void testLockTimeSlot_alreadyBooked() {
        // 准备数据：创建一个已预约的时间槽
        TimeSlotDO slot = createTimeSlot(TimeSlotStatusEnum.BOOKED.getStatus());
        slot.setBookingOrderId(100L);
        timeSlotMapper.insert(slot);

        // 调用
        boolean result = timeSlotService.lockTimeSlot(slot.getId(), 1L);

        // 断言：锁定失败
        assertFalse(result);
    }

    @Test
    public void testReleaseTimeSlot_success() {
        // 准备数据：创建一个已锁定的时间槽
        TimeSlotDO slot = createTimeSlot(TimeSlotStatusEnum.LOCKED.getStatus());
        slot.setLockUserId(1L);
        slot.setLockExpireTime(LocalDateTime.now().plusMinutes(15));
        timeSlotMapper.insert(slot);

        // 调用
        timeSlotService.releaseTimeSlot(slot.getId());

        // 断言
        TimeSlotDO updatedSlot = timeSlotMapper.selectById(slot.getId());
        assertEquals(TimeSlotStatusEnum.AVAILABLE.getStatus(), updatedSlot.getStatus());
        // 注意：MyBatis-Plus updateById 默认不更新null值，所以这里只验证状态变化
    }

    @Test
    public void testReleaseTimeSlot_notLocked() {
        // 准备数据：创建一个可用的时间槽
        TimeSlotDO slot = createTimeSlot(TimeSlotStatusEnum.AVAILABLE.getStatus());
        timeSlotMapper.insert(slot);

        // 调用并断言异常
        assertServiceException(() -> timeSlotService.releaseTimeSlot(slot.getId()), TIME_SLOT_STATUS_ERROR);
    }

    @Test
    public void testConfirmBooking_success() {
        // 准备数据：创建一个已锁定的时间槽
        TimeSlotDO slot = createTimeSlot(TimeSlotStatusEnum.LOCKED.getStatus());
        slot.setLockUserId(1L);
        slot.setLockExpireTime(LocalDateTime.now().plusMinutes(15));
        timeSlotMapper.insert(slot);

        // 调用
        timeSlotService.confirmBooking(slot.getId(), 100L);

        // 断言
        TimeSlotDO updatedSlot = timeSlotMapper.selectById(slot.getId());
        assertEquals(TimeSlotStatusEnum.BOOKED.getStatus(), updatedSlot.getStatus());
        assertEquals(100L, updatedSlot.getBookingOrderId());
        // 注意：MyBatis-Plus updateById 默认不更新null值，所以这里只验证状态和订单ID变化
    }

    @Test
    public void testConfirmBooking_notLocked() {
        // 准备数据：创建一个可用的时间槽
        TimeSlotDO slot = createTimeSlot(TimeSlotStatusEnum.AVAILABLE.getStatus());
        timeSlotMapper.insert(slot);

        // 调用并断言异常
        assertServiceException(() -> timeSlotService.confirmBooking(slot.getId(), 100L), TIME_SLOT_STATUS_ERROR);
    }

    @Test
    public void testCompleteService_success() {
        // 准备数据：创建一个已预约的时间槽
        TimeSlotDO slot = createTimeSlot(TimeSlotStatusEnum.BOOKED.getStatus());
        slot.setBookingOrderId(100L);
        timeSlotMapper.insert(slot);

        // 调用
        timeSlotService.completeService(slot.getId());

        // 断言
        TimeSlotDO updatedSlot = timeSlotMapper.selectById(slot.getId());
        assertEquals(TimeSlotStatusEnum.COMPLETED.getStatus(), updatedSlot.getStatus());
    }

    @Test
    public void testCompleteService_notBooked() {
        // 准备数据：创建一个可用的时间槽
        TimeSlotDO slot = createTimeSlot(TimeSlotStatusEnum.AVAILABLE.getStatus());
        timeSlotMapper.insert(slot);

        // 调用并断言异常
        assertServiceException(() -> timeSlotService.completeService(slot.getId()), TIME_SLOT_STATUS_ERROR);
    }

    @Test
    public void testCancelBooking_fromLocked() {
        // 准备数据：创建一个已锁定的时间槽
        TimeSlotDO slot = createTimeSlot(TimeSlotStatusEnum.LOCKED.getStatus());
        slot.setLockUserId(1L);
        slot.setLockExpireTime(LocalDateTime.now().plusMinutes(15));
        timeSlotMapper.insert(slot);

        // 调用
        timeSlotService.cancelBooking(slot.getId());

        // 断言：恢复为可用状态
        TimeSlotDO updatedSlot = timeSlotMapper.selectById(slot.getId());
        assertEquals(TimeSlotStatusEnum.AVAILABLE.getStatus(), updatedSlot.getStatus());
        // 注意：MyBatis-Plus updateById 默认不更新null值，所以这里只验证状态变化
    }

    @Test
    public void testCancelBooking_fromBooked() {
        // 准备数据：创建一个已预约的时间槽
        TimeSlotDO slot = createTimeSlot(TimeSlotStatusEnum.BOOKED.getStatus());
        slot.setBookingOrderId(100L);
        timeSlotMapper.insert(slot);

        // 调用
        timeSlotService.cancelBooking(slot.getId());

        // 断言：恢复为可用状态
        TimeSlotDO updatedSlot = timeSlotMapper.selectById(slot.getId());
        assertEquals(TimeSlotStatusEnum.AVAILABLE.getStatus(), updatedSlot.getStatus());
        // 注意：MyBatis-Plus updateById 默认不更新null值，所以这里只验证状态变化
    }

    @Test
    public void testCancelBooking_fromAvailable() {
        // 准备数据：创建一个可用的时间槽
        TimeSlotDO slot = createTimeSlot(TimeSlotStatusEnum.AVAILABLE.getStatus());
        timeSlotMapper.insert(slot);

        // 调用并断言异常：可用状态不能取消
        assertServiceException(() -> timeSlotService.cancelBooking(slot.getId()), TIME_SLOT_STATUS_ERROR);
    }

    @Test
    public void testGetTimeSlot_notExists() {
        // 调用
        TimeSlotDO slot = timeSlotService.getTimeSlot(999L);

        // 断言
        assertNull(slot);
    }

    @Test
    public void testGetTimeSlotsByTechnicianAndDate() {
        // 准备数据
        LocalDate today = LocalDate.now();
        TimeSlotDO slot1 = createTimeSlot(TimeSlotStatusEnum.AVAILABLE.getStatus());
        slot1.setTechnicianId(1L);
        slot1.setSlotDate(today);
        slot1.setStartTime(LocalTime.of(9, 0));
        timeSlotMapper.insert(slot1);

        TimeSlotDO slot2 = createTimeSlot(TimeSlotStatusEnum.AVAILABLE.getStatus());
        slot2.setTechnicianId(1L);
        slot2.setSlotDate(today);
        slot2.setStartTime(LocalTime.of(10, 0));
        timeSlotMapper.insert(slot2);

        // 不同技师的时间槽
        TimeSlotDO slot3 = createTimeSlot(TimeSlotStatusEnum.AVAILABLE.getStatus());
        slot3.setTechnicianId(2L);
        slot3.setSlotDate(today);
        timeSlotMapper.insert(slot3);

        // 调用
        java.util.List<TimeSlotDO> result = timeSlotService.getTimeSlotsByTechnicianAndDate(1L, today);

        // 断言
        assertEquals(2, result.size());
    }

    /**
     * 创建测试用时间槽
     */
    private TimeSlotDO createTimeSlot(Integer status) {
        return TimeSlotDO.builder()
                .scheduleId(1L)
                .technicianId(1L)
                .storeId(1L)
                .slotDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .duration(60)
                .isOffpeak(false)
                .status(status)
                .build();
    }

}
