package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxRetryReqVO;
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
        BookingReviewNotifyOutboxDO outbox = buildOutbox(1001L, "BLOCKED_NO_OWNER", "WECOM", null, "wecom-manager-1001");
        outbox.setLastErrorMsg("BLOCKED_NO_OWNER:NO_WECOM_ACCOUNT");
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
        assertEquals("BLOCKED_NO_WECOM_ACCOUNT", result.getData().get(0).getDiagnosticCode());
        assertEquals("缺店长企微账号", result.getData().get(0).getDiagnosticLabel());
        assertEquals("wecom-manager-1001", result.getData().get(0).getReceiverAccount());
        verify(bookingReviewNotifyOutboxService).getNotifyOutboxList(2001L, "BLOCKED_NO_OWNER", 5);
    }

    @Test
    void shouldGetNotifyOutboxPage() {
        BookingReviewNotifyOutboxPageReqVO reqVO = new BookingReviewNotifyOutboxPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setReviewId(2002L);
        reqVO.setStatus("PENDING");
        reqVO.setChannel("WECOM");
        reqVO.setLastActionCode("MANUAL_RETRY");

        BookingReviewNotifyOutboxDO outbox = buildOutbox(1002L, "PENDING", "WECOM", null, "wecom-manager-1002");
        outbox.setLastActionCode("MANUAL_RETRY");
        outbox.setLastActionBizNo("ADMIN#88/OUTBOX#1002");
        outbox.setLastErrorMsg("manual-retry:ops-retry");
        when(bookingReviewNotifyOutboxService.getNotifyOutboxPage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(outbox), 1L));

        CommonResult<PageResult<BookingReviewNotifyOutboxRespVO>> result = controller.page(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals(2002L, result.getData().getList().get(0).getReviewId());
        assertEquals("READY_TO_DISPATCH", result.getData().getList().get(0).getDiagnosticCode());
        assertEquals("待派发", result.getData().getList().get(0).getDiagnosticLabel());
        assertEquals("WECOM", result.getData().getList().get(0).getChannel());
        assertEquals("wecom-manager-1002", result.getData().getList().get(0).getReceiverAccount());
        assertEquals("人工重新入队", result.getData().getList().get(0).getActionLabel());
        assertEquals("管理员#88", result.getData().getList().get(0).getActionOperatorLabel());
        assertEquals("ops-retry", result.getData().getList().get(0).getActionReason());
        verify(bookingReviewNotifyOutboxService).getNotifyOutboxPage(reqVO);
    }

    @Test
    void shouldRetryNotifyOutbox() {
        BookingReviewNotifyOutboxRetryReqVO reqVO = new BookingReviewNotifyOutboxRetryReqVO();
        reqVO.setIds(List.of(1001L));
        reqVO.setReason("manual-retry");
        when(bookingReviewNotifyOutboxService.retryNotifyOutbox(reqVO.getIds(), null, reqVO.getReason()))
                .thenReturn(1);

        CommonResult<Integer> result = controller.retry(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData());
        verify(bookingReviewNotifyOutboxService).retryNotifyOutbox(reqVO.getIds(), null, reqVO.getReason());
    }

    private BookingReviewNotifyOutboxDO buildOutbox(Long id, String status, String channel,
                                                     Long receiverUserId, String receiverAccount) {
        return new BookingReviewNotifyOutboxDO()
                .setId(id)
                .setBizType("BOOKING_REVIEW_NEGATIVE")
                .setBizId(2000L + (id - 1000L))
                .setStoreId(3001L)
                .setReceiverRole("STORE_MANAGER")
                .setReceiverUserId(receiverUserId)
                .setReceiverAccount(receiverAccount)
                .setNotifyType("NEGATIVE_REVIEW_CREATED")
                .setChannel(channel)
                .setStatus(status)
                .setRetryCount(0)
                .setLastErrorMsg("BLOCKED_NO_OWNER".equals(status) ? "BLOCKED_NO_OWNER:NO_OWNER" : "")
                .setLastActionCode("BLOCKED_NO_OWNER".equals(status) ? "BLOCKED_NO_OWNER" : "CREATE_OUTBOX")
                .setLastActionTime(LocalDateTime.now().withNano(0));
    }
}
