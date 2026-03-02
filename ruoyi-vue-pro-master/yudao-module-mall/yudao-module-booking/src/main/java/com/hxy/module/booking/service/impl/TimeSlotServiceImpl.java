package com.hxy.module.booking.service.impl;

import com.hxy.module.booking.dal.dataobject.TimeSlotDO;
import com.hxy.module.booking.dal.mysql.TimeSlotMapper;
import com.hxy.module.booking.enums.TimeSlotStatusEnum;
import com.hxy.module.booking.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.*;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class TimeSlotServiceImpl implements TimeSlotService {

    private final TimeSlotMapper timeSlotMapper;

    /**
     * 锁定超时时间（分钟）
     */
    private static final int LOCK_TIMEOUT_MINUTES = 15;

    @Override
    public TimeSlotDO getTimeSlot(Long id) {
        return timeSlotMapper.selectById(id);
    }

    @Override
    public List<TimeSlotDO> getAvailableTimeSlotsByStoreAndDate(Long storeId, LocalDate date) {
        return timeSlotMapper.selectAvailableByStoreIdAndDate(storeId, date);
    }

    @Override
    public List<TimeSlotDO> getTimeSlotsByTechnicianAndDate(Long technicianId, LocalDate date) {
        return timeSlotMapper.selectListByTechnicianIdAndDate(technicianId, date);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockTimeSlot(Long slotId, Long userId) {
        // 使用CAS更新，避免并发问题
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(LOCK_TIMEOUT_MINUTES);
        int updated = timeSlotMapper.lockTimeSlot(
                slotId,
                TimeSlotStatusEnum.AVAILABLE.getStatus(),
                TimeSlotStatusEnum.LOCKED.getStatus(),
                userId,
                expireTime
        );
        if (updated == 0) {
            log.warn("锁定时间槽失败，slotId={}, userId={}", slotId, userId);
            return false;
        }
        log.info("锁定时间槽成功，slotId={}, userId={}, expireTime={}", slotId, userId, expireTime);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseTimeSlot(Long slotId) {
        TimeSlotDO slot = validateTimeSlotExists(slotId);
        if (!TimeSlotStatusEnum.LOCKED.getStatus().equals(slot.getStatus())) {
            throw exception(TIME_SLOT_STATUS_ERROR);
        }
        TimeSlotDO update = new TimeSlotDO();
        update.setId(slotId);
        update.setStatus(TimeSlotStatusEnum.AVAILABLE.getStatus());
        update.setLockUserId(null);
        update.setLockExpireTime(null);
        timeSlotMapper.updateById(update);
        log.info("释放时间槽锁定，slotId={}", slotId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBooking(Long slotId, Long bookingOrderId) {
        TimeSlotDO slot = validateTimeSlotExists(slotId);
        if (!TimeSlotStatusEnum.LOCKED.getStatus().equals(slot.getStatus())) {
            throw exception(TIME_SLOT_STATUS_ERROR);
        }
        TimeSlotDO update = new TimeSlotDO();
        update.setId(slotId);
        update.setStatus(TimeSlotStatusEnum.BOOKED.getStatus());
        update.setBookingOrderId(bookingOrderId);
        update.setLockExpireTime(null);
        timeSlotMapper.updateById(update);
        log.info("确认预约，slotId={}, bookingOrderId={}", slotId, bookingOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeService(Long slotId) {
        TimeSlotDO slot = validateTimeSlotExists(slotId);
        if (!TimeSlotStatusEnum.BOOKED.getStatus().equals(slot.getStatus())) {
            throw exception(TIME_SLOT_STATUS_ERROR);
        }
        TimeSlotDO update = new TimeSlotDO();
        update.setId(slotId);
        update.setStatus(TimeSlotStatusEnum.COMPLETED.getStatus());
        timeSlotMapper.updateById(update);
        log.info("完成服务，slotId={}", slotId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBooking(Long slotId) {
        TimeSlotDO slot = validateTimeSlotExists(slotId);
        // 只有锁定或已预约状态可以取消
        if (!TimeSlotStatusEnum.LOCKED.getStatus().equals(slot.getStatus())
                && !TimeSlotStatusEnum.BOOKED.getStatus().equals(slot.getStatus())) {
            throw exception(TIME_SLOT_STATUS_ERROR);
        }
        TimeSlotDO update = new TimeSlotDO();
        update.setId(slotId);
        // 取消后恢复为可用状态，以便重新预约
        update.setStatus(TimeSlotStatusEnum.AVAILABLE.getStatus());
        update.setLockUserId(null);
        update.setLockExpireTime(null);
        update.setBookingOrderId(null);
        timeSlotMapper.updateById(update);
        log.info("取消预约，slotId={}", slotId);
    }

    @Override
    public int releaseExpiredLocks() {
        int released = timeSlotMapper.releaseExpiredLocks(LocalDateTime.now());
        if (released > 0) {
            log.info("释放过期锁定的时间槽，数量={}", released);
        }
        return released;
    }

    private TimeSlotDO validateTimeSlotExists(Long id) {
        TimeSlotDO slot = timeSlotMapper.selectById(id);
        if (slot == null) {
            throw exception(TIME_SLOT_NOT_EXISTS);
        }
        return slot;
    }

}
