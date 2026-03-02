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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private TechnicianScheduleMapper scheduleMapper;
    @Mock
    private TimeSlotMapper timeSlotMapper;
    @Mock
    private TechnicianMapper technicianMapper;
    @Mock
    private OffpeakRuleService offpeakRuleService;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    @Test
    void testGenerateTimeSlots_discountRuleShouldNotPersistZeroOffpeakPrice() {
        Long scheduleId = 1L;
        TechnicianScheduleDO schedule = TechnicianScheduleDO.builder()
                .id(scheduleId)
                .technicianId(10L)
                .storeId(100L)
                .scheduleDate(LocalDate.of(2026, 2, 24))
                .weekDay(2)
                .isRestDay(false)
                .workStartTime(LocalTime.of(9, 0))
                .workEndTime(LocalTime.of(10, 0))
                .build();
        when(scheduleMapper.selectById(eq(scheduleId))).thenReturn(schedule);
        when(timeSlotMapper.selectListByScheduleId(eq(scheduleId))).thenReturn(Collections.emptyList());

        OffpeakRuleDO rule = OffpeakRuleDO.builder()
                .discountRate(80)
                .build();
        when(offpeakRuleService.matchOffpeakRule(eq(100L), eq(2), eq(LocalTime.of(9, 0))))
                .thenReturn(rule);

        scheduleService.generateTimeSlots(scheduleId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<TimeSlotDO>> batchCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(timeSlotMapper).insertBatch(batchCaptor.capture());
        Collection<TimeSlotDO> createdSlots = batchCaptor.getValue();
        assertEquals(1, createdSlots.size());
        TimeSlotDO createdSlot = createdSlots.iterator().next();
        assertTrue(Boolean.TRUE.equals(createdSlot.getIsOffpeak()));
        assertNull(createdSlot.getOffpeakPrice());
        verify(offpeakRuleService, never()).calculateOffpeakPrice(any(), any());
    }

    @Test
    void testGenerateTimeSlots_multipleSlots() {
        Long scheduleId = 2L;
        TechnicianScheduleDO schedule = TechnicianScheduleDO.builder()
                .id(scheduleId)
                .technicianId(10L)
                .storeId(100L)
                .scheduleDate(LocalDate.of(2026, 3, 1))
                .weekDay(7)
                .isRestDay(false)
                .workStartTime(LocalTime.of(9, 0))
                .workEndTime(LocalTime.of(12, 0))
                .build();
        when(scheduleMapper.selectById(eq(scheduleId))).thenReturn(schedule);
        when(timeSlotMapper.selectListByScheduleId(eq(scheduleId))).thenReturn(Collections.emptyList());

        scheduleService.generateTimeSlots(scheduleId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<TimeSlotDO>> batchCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(timeSlotMapper).insertBatch(batchCaptor.capture());
        Collection<TimeSlotDO> slots = batchCaptor.getValue();
        assertEquals(3, slots.size()); // 9-10, 10-11, 11-12
    }

    @Test
    void testGenerateTimeSlots_restDaySkipped() {
        Long scheduleId = 3L;
        TechnicianScheduleDO schedule = TechnicianScheduleDO.builder()
                .id(scheduleId)
                .technicianId(10L)
                .storeId(100L)
                .scheduleDate(LocalDate.of(2026, 3, 1))
                .weekDay(7)
                .isRestDay(true)
                .workStartTime(LocalTime.of(9, 0))
                .workEndTime(LocalTime.of(21, 0))
                .build();
        when(scheduleMapper.selectById(eq(scheduleId))).thenReturn(schedule);

        scheduleService.generateTimeSlots(scheduleId);

        verify(timeSlotMapper, never()).insertBatch(any());
    }

    @Test
    void testGenerateTimeSlots_nullWorkTimesUsesDefaults() {
        Long scheduleId = 4L;
        TechnicianScheduleDO schedule = TechnicianScheduleDO.builder()
                .id(scheduleId)
                .technicianId(10L)
                .storeId(100L)
                .scheduleDate(LocalDate.of(2026, 3, 1))
                .weekDay(7)
                .isRestDay(false)
                .workStartTime(null)
                .workEndTime(null)
                .build();
        when(scheduleMapper.selectById(eq(scheduleId))).thenReturn(schedule);
        when(timeSlotMapper.selectListByScheduleId(eq(scheduleId))).thenReturn(Collections.emptyList());

        scheduleService.generateTimeSlots(scheduleId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<TimeSlotDO>> batchCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(timeSlotMapper).insertBatch(batchCaptor.capture());
        Collection<TimeSlotDO> slots = batchCaptor.getValue();
        // Default 9:00-21:00 = 12 slots
        assertEquals(12, slots.size());
    }

    @Test
    void testBatchCreateSchedule_success() {
        Long technicianId = 10L;
        TechnicianDO technician = new TechnicianDO();
        technician.setId(technicianId);
        technician.setStoreId(100L);
        when(technicianMapper.selectById(eq(technicianId))).thenReturn(technician);

        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 3);

        // No existing schedules
        when(scheduleMapper.selectByTechnicianIdAndDate(eq(technicianId), any(LocalDate.class))).thenReturn(null);
        // Mock schedule insert to set ID for generateTimeSlots
        doAnswer(invocation -> {
            TechnicianScheduleDO s = invocation.getArgument(0);
            s.setId(System.nanoTime());
            return 1;
        }).when(scheduleMapper).insert(any(TechnicianScheduleDO.class));
        when(timeSlotMapper.selectListByScheduleId(any())).thenReturn(Collections.emptyList());

        scheduleService.batchCreateSchedule(technicianId, start, end);

        // 3 days = 3 schedules created
        verify(scheduleMapper, times(3)).insert(any(TechnicianScheduleDO.class));
    }

    @Test
    void testBatchCreateSchedule_skipsExisting() {
        Long technicianId = 10L;
        TechnicianDO technician = new TechnicianDO();
        technician.setId(technicianId);
        technician.setStoreId(100L);
        when(technicianMapper.selectById(eq(technicianId))).thenReturn(technician);

        LocalDate date = LocalDate.of(2026, 3, 1);

        // Existing schedule for this date
        TechnicianScheduleDO existing = TechnicianScheduleDO.builder().id(99L).build();
        when(scheduleMapper.selectByTechnicianIdAndDate(eq(technicianId), eq(date))).thenReturn(existing);

        scheduleService.batchCreateSchedule(technicianId, date, date);

        // Should not insert
        verify(scheduleMapper, never()).insert(any(TechnicianScheduleDO.class));
    }
}
