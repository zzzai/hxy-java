package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewCreateReqVO;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.dal.mysql.BookingOrderMapper;
import com.hxy.module.booking.dal.mysql.BookingReviewMapper;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_REVIEW_ALREADY_EXISTS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingReviewServiceImplRaceConditionTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BookingReviewServiceImpl bookingReviewService;

    @Mock
    private BookingReviewMapper bookingReviewMapper;
    @Mock
    private BookingOrderMapper bookingOrderMapper;

    @Test
    void shouldTranslateDuplicateInsertIntoAlreadyExists() {
        BookingOrderDO order = BookingOrderDO.builder()
                .id(1001L)
                .userId(2001L)
                .storeId(3001L)
                .technicianId(4001L)
                .spuId(5001L)
                .skuId(6001L)
                .status(BookingOrderStatusEnum.COMPLETED.getStatus())
                .serviceEndTime(LocalDateTime.now().minusMinutes(5).withNano(0))
                .build();
        when(bookingOrderMapper.selectById(1001L)).thenReturn(order);
        when(bookingReviewMapper.selectByBookingOrderId(1001L)).thenReturn(null);
        when(bookingReviewMapper.insert(any(BookingReviewDO.class)))
                .thenThrow(new DuplicateKeyException("uk_booking_review_order"));

        AppBookingReviewCreateReqVO reqVO = new AppBookingReviewCreateReqVO();
        reqVO.setBookingOrderId(1001L);
        reqVO.setOverallScore(4);

        assertServiceException(() -> bookingReviewService.createReview(2001L, reqVO), BOOKING_REVIEW_ALREADY_EXISTS);

        verify(bookingReviewMapper).insert(any(BookingReviewDO.class));
    }
}
