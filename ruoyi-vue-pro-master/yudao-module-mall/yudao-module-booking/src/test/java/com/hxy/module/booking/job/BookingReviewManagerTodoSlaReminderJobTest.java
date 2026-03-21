package com.hxy.module.booking.job;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.service.BookingReviewNotifyOutboxService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingReviewManagerTodoSlaReminderJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BookingReviewManagerTodoSlaReminderJob job;

    @Mock
    private BookingReviewNotifyOutboxService bookingReviewNotifyOutboxService;

    @Test
    void shouldExecuteWithDefaultParam() {
        when(bookingReviewNotifyOutboxService.createManagerTodoSlaReminderOutbox(200)).thenReturn(3);

        String result = job.execute(null);

        assertTrue(result.contains("3"));
        verify(bookingReviewNotifyOutboxService).createManagerTodoSlaReminderOutbox(200);
    }

    @Test
    void shouldExecuteWithCustomParam() {
        when(bookingReviewNotifyOutboxService.createManagerTodoSlaReminderOutbox(50)).thenReturn(1);

        String result = job.execute("50");

        assertTrue(result.contains("1"));
        verify(bookingReviewNotifyOutboxService).createManagerTodoSlaReminderOutbox(50);
    }
}
