package com.hxy.module.booking.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.app.vo.AppTimeSlotRespVO;
import com.hxy.module.booking.dal.dataobject.TimeSlotDO;
import com.hxy.module.booking.service.TimeSlotService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppTimeSlotControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppTimeSlotController controller;

    @Mock
    private TimeSlotService timeSlotService;

    @Test
    void getTimeSlot_shouldExposeDuration() {
        TimeSlotDO slot = TimeSlotDO.builder()
                .id(1001L)
                .duration(90)
                .build();
        when(timeSlotService.getTimeSlot(1001L)).thenReturn(slot);

        CommonResult<AppTimeSlotRespVO> result = controller.getTimeSlot(1001L);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(90, result.getData().getDuration());
        verify(timeSlotService).getTimeSlot(1001L);
    }
}
