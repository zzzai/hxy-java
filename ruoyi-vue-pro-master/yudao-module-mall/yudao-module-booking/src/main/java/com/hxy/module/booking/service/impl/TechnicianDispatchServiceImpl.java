package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import com.hxy.module.booking.dal.dataobject.TechnicianDO;
import com.hxy.module.booking.dal.dataobject.TimeSlotDO;
import com.hxy.module.booking.dal.mysql.TechnicianMapper;
import com.hxy.module.booking.dal.mysql.TimeSlotMapper;
import com.hxy.module.booking.enums.TimeSlotStatusEnum;
import com.hxy.module.booking.service.TechnicianDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 技师派单（排钟）Service 实现
 * 算法：按当日已接单数升序 + 评分降序，选择有空闲时段的最优技师
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicianDispatchServiceImpl implements TechnicianDispatchService {

    private final TechnicianMapper technicianMapper;
    private final TimeSlotMapper timeSlotMapper;

    @Override
    public TechnicianDO autoAssignTechnician(Long storeId, LocalDate date, LocalTime startTime) {
        // 1. 获取门店所有启用的技师
        List<TechnicianDO> technicians = technicianMapper.selectListByStoreIdAndStatus(
                storeId, CommonStatusEnum.ENABLE.getStatus());
        if (technicians.isEmpty()) {
            log.warn("排钟失败：门店无可用技师，storeId={}", storeId);
            return null;
        }

        // 2. 过滤出在指定时段有空闲 slot 的技师
        List<TechnicianDO> available = technicians.stream()
                .filter(tech -> hasAvailableSlot(tech.getId(), date, startTime))
                .collect(Collectors.toList());
        if (available.isEmpty()) {
            log.warn("排钟失败：无技师在指定时段有空闲，storeId={}, date={}, startTime={}",
                    storeId, date, startTime);
            return null;
        }

        // 3. 按当日已接单数升序 + 评分降序排序
        available.sort(Comparator
                .comparingInt((TechnicianDO tech) -> countBookedSlots(tech.getId(), date))
                .thenComparing(TechnicianDO::getRating, Comparator.reverseOrder()));

        TechnicianDO selected = available.get(0);
        log.info("排钟分配技师：technicianId={}, name={}, storeId={}, date={}, startTime={}",
                selected.getId(), selected.getName(), storeId, date, startTime);
        return selected;
    }

    private boolean hasAvailableSlot(Long technicianId, LocalDate date, LocalTime startTime) {
        List<TimeSlotDO> slots = timeSlotMapper.selectListByTechnicianIdAndDate(technicianId, date);
        return slots.stream().anyMatch(slot ->
                TimeSlotStatusEnum.AVAILABLE.getStatus().equals(slot.getStatus())
                        && slot.getStartTime().equals(startTime));
    }

    private int countBookedSlots(Long technicianId, LocalDate date) {
        List<TimeSlotDO> slots = timeSlotMapper.selectListByTechnicianIdAndDate(technicianId, date);
        return (int) slots.stream()
                .filter(slot -> TimeSlotStatusEnum.BOOKED.getStatus().equals(slot.getStatus())
                        || TimeSlotStatusEnum.COMPLETED.getStatus().equals(slot.getStatus()))
                .count();
    }

}
