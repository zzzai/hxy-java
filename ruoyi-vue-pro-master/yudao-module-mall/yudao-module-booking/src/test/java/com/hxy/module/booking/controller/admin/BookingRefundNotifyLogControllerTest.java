package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogRespVO;
import com.hxy.module.booking.dal.dataobject.BookingRefundNotifyLogDO;
import com.hxy.module.booking.service.BookingRefundNotifyLogService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingRefundNotifyLogControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BookingRefundNotifyLogController controller;

    @Mock
    private BookingRefundNotifyLogService refundNotifyLogService;

    @Test
    void page_shouldDelegateService() {
        BookingRefundNotifyLogPageReqVO reqVO = new BookingRefundNotifyLogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setStatus("fail");

        BookingRefundNotifyLogDO row = new BookingRefundNotifyLogDO()
                .setId(1L)
                .setOrderId(1001L)
                .setMerchantRefundId("1001-refund")
                .setPayRefundId(9001L)
                .setStatus("fail");
        when(refundNotifyLogService.getNotifyLogPage(eq(reqVO)))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        CommonResult<PageResult<BookingRefundNotifyLogRespVO>> result = controller.page(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1L, result.getData().getList().get(0).getId());
        assertEquals(1001L, result.getData().getList().get(0).getOrderId());
        verify(refundNotifyLogService).getNotifyLogPage(eq(reqVO));
    }

    @Test
    void replay_shouldDelegateService() {
        BookingRefundNotifyLogReplayReqVO reqVO = new BookingRefundNotifyLogReplayReqVO();
        reqVO.setId(10L);

        CommonResult<Boolean> result = controller.replay(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(refundNotifyLogService).replayFailedLog(eq(10L), isNull());
    }
}
