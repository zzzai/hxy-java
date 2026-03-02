package com.hxy.module.booking.service.impl;

import com.hxy.module.booking.dal.dataobject.OffpeakRuleDO;
import com.hxy.module.booking.dal.mysql.OffpeakRuleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OffpeakRuleServiceImplTest {

    @Mock
    private OffpeakRuleMapper offpeakRuleMapper;

    @InjectMocks
    private OffpeakRuleServiceImpl offpeakRuleService;

    @Test
    void testMatchOffpeakRule_matchesWeekDayAndTime() {
        OffpeakRuleDO rule = OffpeakRuleDO.builder()
                .id(1L)
                .storeId(100L)
                .weekDays("1,2,3")
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(17, 0))
                .discountRate(80)
                .build();
        when(offpeakRuleMapper.selectEnabledByStoreId(eq(100L))).thenReturn(Arrays.asList(rule));

        OffpeakRuleDO result = offpeakRuleService.matchOffpeakRule(100L, 2, LocalTime.of(14, 0));

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testMatchOffpeakRule_noMatchWeekDay() {
        OffpeakRuleDO rule = OffpeakRuleDO.builder()
                .id(1L)
                .storeId(100L)
                .weekDays("1,2,3")
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(17, 0))
                .build();
        when(offpeakRuleMapper.selectEnabledByStoreId(eq(100L))).thenReturn(Arrays.asList(rule));

        OffpeakRuleDO result = offpeakRuleService.matchOffpeakRule(100L, 5, LocalTime.of(14, 0));

        assertNull(result);
    }

    @Test
    void testMatchOffpeakRule_noMatchTime() {
        OffpeakRuleDO rule = OffpeakRuleDO.builder()
                .id(1L)
                .storeId(100L)
                .weekDays("1,2,3,4,5")
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(17, 0))
                .build();
        when(offpeakRuleMapper.selectEnabledByStoreId(eq(100L))).thenReturn(Arrays.asList(rule));

        OffpeakRuleDO result = offpeakRuleService.matchOffpeakRule(100L, 2, LocalTime.of(10, 0));

        assertNull(result);
    }

    @Test
    void testMatchOffpeakRule_noRules() {
        when(offpeakRuleMapper.selectEnabledByStoreId(eq(100L))).thenReturn(Collections.emptyList());

        OffpeakRuleDO result = offpeakRuleService.matchOffpeakRule(100L, 2, LocalTime.of(14, 0));

        assertNull(result);
    }

    @Test
    void testCalculateOffpeakPrice_fixedPrice() {
        OffpeakRuleDO rule = OffpeakRuleDO.builder()
                .fixedPrice(5000)
                .discountRate(80)
                .build();

        Integer price = offpeakRuleService.calculateOffpeakPrice(rule, 10000);

        assertEquals(5000, price);
    }

    @Test
    void testCalculateOffpeakPrice_discountRate() {
        OffpeakRuleDO rule = OffpeakRuleDO.builder()
                .discountRate(80)
                .build();

        Integer price = offpeakRuleService.calculateOffpeakPrice(rule, 10000);

        assertEquals(8000, price);
    }

    @Test
    void testCalculateOffpeakPrice_nullRule() {
        Integer price = offpeakRuleService.calculateOffpeakPrice(null, 10000);

        assertEquals(10000, price);
    }

    @Test
    void testCalculateOffpeakPrice_noFixedNoDiscount() {
        OffpeakRuleDO rule = OffpeakRuleDO.builder().build();

        Integer price = offpeakRuleService.calculateOffpeakPrice(rule, 10000);

        assertEquals(10000, price);
    }

    @Test
    void testMatchOffpeakRule_boundaryTime() {
        OffpeakRuleDO rule = OffpeakRuleDO.builder()
                .id(1L)
                .storeId(100L)
                .weekDays("1,2,3,4,5,6,7")
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(17, 0))
                .build();
        when(offpeakRuleMapper.selectEnabledByStoreId(eq(100L))).thenReturn(Arrays.asList(rule));

        // Exact start time should match
        assertNotNull(offpeakRuleService.matchOffpeakRule(100L, 1, LocalTime.of(13, 0)));
        // Exact end time should match
        assertNotNull(offpeakRuleService.matchOffpeakRule(100L, 1, LocalTime.of(17, 0)));
    }
}
