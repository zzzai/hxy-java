package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewNotifyOutboxDO;
import com.hxy.module.booking.service.BookingReviewNotifyOutboxService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingReviewNotifyOutboxControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BookingReviewNotifyOutboxController controller;

    @Mock
    private BookingReviewNotifyOutboxService bookingReviewNotifyOutboxService;

    @Test
    void shouldGetNotifyOutboxListByReviewId() {
        BookingReviewNotifyOutboxDO outbox = buildOutbox(1001L, "BLOCKED_NO_OWNER");
        when(bookingReviewNotifyOutboxService.getNotifyOutboxList(2001L, "BLOCKED_NO_OWNER", 5))
                .thenReturn(Collections.singletonList(outbox));

        CommonResult<List<BookingReviewNotifyOutboxRespVO>> result =
                controller.list(2001L, "BLOCKED_NO_OWNER", 5);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals(1001L, result.getData().get(0).getId());
        assertEquals(2001L, result.getData().get(0).getReviewId());
        assertEquals("BLOCKED_NO_OWNER", result.getData().get(0).getStatus());
        verify(bookingReviewNotifyOutboxService).getNotifyOutboxList(2001L, "BLOCKED_NO_OWNER", 5);
    }

    @Test
    void shouldGetNotifyOutboxPage() {
        BookingReviewNotifyOutboxPageReqVO reqVO = new BookingReviewNotifyOutboxPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setReviewId(2002L);
        reqVO.setStatus("PENDING");

        BookingReviewNotifyOutboxDO outbox = buildOutbox(1002L, "PENDING");
        when(bookingReviewNotifyOutboxService.getNotifyOutboxPage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(outbox), 1L));

        CommonResult<PageResult<BookingReviewNotifyOutboxRespVO>> result = controller.page(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals(2002L, result.getData().getList().get(0).getReviewId());
        verify(bookingReviewNotifyOutboxService).getNotifyOutboxPage(reqVO);
    }

    private BookingReviewNotifyOutboxDO buildOutbox(Long id, String status) {
        return new BookingReviewNotifyOutboxDO()
                .setId(id)
                .setBizType("BOOKING_REVIEW_NEGATIVE")
                .setBizId(2000L + (id - 1000L))
                .setStoreId(3001L)
                .setReceiverRole("STORE_MANAGER")
                .setReceiverUserId(9001L)
                .setNotifyType("NEGATIVE_REVIEW_CREATED")
                .setChannel("IN_APP")
                .setStatus(status)
                .setRetryCount(0)
                .setLastErrorMsg("BLOCKED_NO_OWNER".equals(status) ? "BLOCKED_NO_OWNER:NO_OWNER" : "")
                .setLastActionCode("BLOCKED_NO_OWNER".equals(status) ? "BLOCKED_NO_OWNER" : "CREATE_OUTBOX")
                .setLastActionTime(LocalDateTime.now().withNano(0));
    }
}
