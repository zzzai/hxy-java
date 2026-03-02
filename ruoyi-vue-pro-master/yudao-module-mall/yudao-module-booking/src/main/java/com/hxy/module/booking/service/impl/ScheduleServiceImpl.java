package com.hxy.module.booking.service.impl;

import com.hxy.module.booking.dal.dataobject.OffpeakRuleDO;
import com.hxy.module.booking.dal.dataobject.TechnicianDO;
import com.hxy.module.booking.dal.dataobject.TechnicianScheduleDO;
import com.hxy.module.booking.dal.dataobject.TimeSlotDO;
import com.hxy.module.booking.dal.mysql.TechnicianMapper;
import com.hxy.module.booking.dal.mysql.TechnicianScheduleMapper;
import com.hxy.module.booking.dal.mysql.TimeSlotMapper;
import com.hxy.module.booking.enums.TimeSlotStatusEnum;
import com.hxy.module.booking.service.OffpeakRuleService;
import com.hxy.module.booking.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.*;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {

    private final TechnicianScheduleMapper scheduleMapper;
    private final TimeSlotMapper timeSlotMapper;
    private final TechnicianMapper technicianMapper;
    private final OffpeakRuleService offpeakRuleService;

    /**
     * 默认工作开始时间
     */
    private static final LocalTime DEFAULT_WORK_START_TIME = LocalTime.of(9, 0);

    /**
     * 默认工作结束时间
     */
    private static final LocalTime DEFAULT_WORK_END_TIME = LocalTime.of(21, 0);

    /**
     * 默认时间槽时长（分钟）
     */
    private static final int DEFAULT_SLOT_DURATION = 60;

    @Override
    public Long createSchedule(TechnicianScheduleDO schedule) {
        // 检查是否已存在排班
        TechnicianScheduleDO existing = scheduleMapper.selectByTechnicianIdAndDate(
                schedule.getTechnicianId(), schedule.getScheduleDate());
        if (existing != null) {
            throw exception(SCHEDULE_ALREADY_EXISTS);
        }
        scheduleMapper.insert(schedule);
        return schedule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreateSchedule(Long technicianId, LocalDate startDate, LocalDate endDate) {
        TechnicianDO technician = technicianMapper.selectById(technicianId);
        if (technician == null) {
            throw exception(TECHNICIAN_NOT_EXISTS);
        }

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            // 检查是否已存在排班
            TechnicianScheduleDO existing = scheduleMapper.selectByTechnicianIdAndDate(technicianId, current);
            if (existing == null) {
                // 创建排班
                TechnicianScheduleDO schedule = TechnicianScheduleDO.builder()
                        .technicianId(technicianId)
                        .storeId(technician.getStoreId())
                        .scheduleDate(current)
                        .weekDay(current.getDayOfWeek().getValue() % 7) // 0=周日
                        .isRestDay(false)
                        .workStartTime(DEFAULT_WORK_START_TIME)
                        .workEndTime(DEFAULT_WORK_END_TIME)
                        .build();
                scheduleMapper.insert(schedule);
                // 生成时间槽
                generateTimeSlots(schedule.getId());
            }
            current = current.plusDays(1);
        }
        log.info("批量创建排班完成，technicianId={}, startDate={}, endDate={}", technicianId, startDate, endDate);
    }

    @Override
    public void updateSchedule(TechnicianScheduleDO schedule) {
        validateScheduleExists(schedule.getId());
        scheduleMapper.updateById(schedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Long id) {
        validateScheduleExists(id);
        // 删除关联的时间槽
        List<TimeSlotDO> slots = timeSlotMapper.selectListByScheduleId(id);
        for (TimeSlotDO slot : slots) {
            // 只能删除可用状态的时间槽
            if (TimeSlotStatusEnum.AVAILABLE.getStatus().equals(slot.getStatus())) {
                timeSlotMapper.deleteById(slot.getId());
            }
        }
        scheduleMapper.deleteById(id);
    }

    @Override
    public TechnicianScheduleDO getSchedule(Long id) {
        return scheduleMapper.selectById(id);
    }

    @Override
    public TechnicianScheduleDO getScheduleByTechnicianAndDate(Long technicianId, LocalDate date) {
        return scheduleMapper.selectByTechnicianIdAndDate(technicianId, date);
    }

    @Override
    public List<TechnicianScheduleDO> getScheduleListByTechnicianAndDateRange(Long technicianId, LocalDate startDate, LocalDate endDate) {
        return scheduleMapper.selectListByTechnicianIdAndDateRange(technicianId, startDate, endDate);
    }

    @Override
    public List<TechnicianScheduleDO> getScheduleListByStoreAndDate(Long storeId, LocalDate date) {
        return scheduleMapper.selectListByStoreIdAndDate(storeId, date);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setRestDay(Long scheduleId, Boolean isRestDay, String remark) {
        TechnicianScheduleDO schedule = validateScheduleExists(scheduleId);

        // 如果设置为休息日，需要取消该日所有可用时间槽
        if (Boolean.TRUE.equals(isRestDay)) {
            List<TimeSlotDO> slots = timeSlotMapper.selectListByScheduleId(scheduleId);
            for (TimeSlotDO slot : slots) {
                if (TimeSlotStatusEnum.AVAILABLE.getStatus().equals(slot.getStatus())) {
                    TimeSlotDO update = new TimeSlotDO();
                    update.setId(slot.getId());
                    update.setStatus(TimeSlotStatusEnum.CANCELLED.getStatus());
                    timeSlotMapper.updateById(update);
                }
            }
        }

        TechnicianScheduleDO update = new TechnicianScheduleDO();
        update.setId(scheduleId);
        update.setIsRestDay(isRestDay);
        update.setRemark(remark);
        scheduleMapper.updateById(update);
        log.info("设置休息日，scheduleId={}, isRestDay={}, remark={}", scheduleId, isRestDay, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateTimeSlots(Long scheduleId) {
        TechnicianScheduleDO schedule = validateScheduleExists(scheduleId);

        // 休息日不生成时间槽
        if (Boolean.TRUE.equals(schedule.getIsRestDay())) {
            return;
        }

        // 幂等性检查：如果已存在时间槽则不重复生成
        List<TimeSlotDO> existingSlots = timeSlotMapper.selectListByScheduleId(scheduleId);
        if (!existingSlots.isEmpty()) {
            log.warn("时间槽已存在，跳过生成，scheduleId={}, existingCount={}", scheduleId, existingSlots.size());
            throw exception(TIME_SLOT_ALREADY_GENERATED);
        }

        LocalTime current = schedule.getWorkStartTime() != null ? schedule.getWorkStartTime() : DEFAULT_WORK_START_TIME;
        LocalTime endTime = schedule.getWorkEndTime() != null ? schedule.getWorkEndTime() : DEFAULT_WORK_END_TIME;
        List<TimeSlotDO> slots = new ArrayList<>();

        while (current.plusMinutes(DEFAULT_SLOT_DURATION).compareTo(endTime) <= 0) {
            LocalTime slotEnd = current.plusMinutes(DEFAULT_SLOT_DURATION);

            // 判断是否为闲时
            int weekDay = schedule.getWeekDay() == 0 ? 7 : schedule.getWeekDay(); // 转换为1-7
            OffpeakRuleDO offpeakRule = offpeakRuleService.matchOffpeakRule(schedule.getStoreId(), weekDay, current);
            boolean isOffpeak = offpeakRule != null;
            Integer offpeakPrice = resolveOffpeakPrice(offpeakRule);

            TimeSlotDO slot = TimeSlotDO.builder()
                    .scheduleId(scheduleId)
                    .technicianId(schedule.getTechnicianId())
                    .storeId(schedule.getStoreId())
                    .slotDate(schedule.getScheduleDate())
                    .startTime(current)
                    .endTime(slotEnd)
                    .duration(DEFAULT_SLOT_DURATION)
                    .isOffpeak(isOffpeak)
                    .offpeakPrice(offpeakPrice)
                    .status(TimeSlotStatusEnum.AVAILABLE.getStatus())
                    .build();
            slots.add(slot);

            current = slotEnd;
        }

        // 批量插入
        if (!slots.isEmpty()) {
            timeSlotMapper.insertBatch(slots);
        }
        log.info("生成时间槽完成，scheduleId={}, count={}", scheduleId, slots.size());
    }

    private Integer resolveOffpeakPrice(OffpeakRuleDO offpeakRule) {
        if (offpeakRule == null) {
            return null;
        }
        Integer fixedPrice = offpeakRule.getFixedPrice();
        // 折扣规则依赖商品原价，生成时间槽阶段不预计算，避免写入0元脏值
        return fixedPrice != null && fixedPrice > 0 ? fixedPrice : null;
    }

    private TechnicianScheduleDO validateScheduleExists(Long id) {
        TechnicianScheduleDO schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw exception(SCHEDULE_NOT_EXISTS);
        }
        return schedule;
    }

}
