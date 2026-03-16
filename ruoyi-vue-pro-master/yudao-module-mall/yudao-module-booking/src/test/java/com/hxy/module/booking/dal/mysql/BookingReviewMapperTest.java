package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MapperScan("com.hxy.module.booking.dal.mysql")
class BookingReviewMapperTest extends BaseDbUnitTest {

    @Resource
    private BookingReviewMapper bookingReviewMapper;

    @Test
    void shouldInsertAndSelectByBookingOrderId() {
        BookingReviewDO review = BookingReviewDO.builder()
                .bookingOrderId(1001L)
                .serviceOrderId(2001L)
                .storeId(3001L)
                .technicianId(4001L)
                .memberId(5001L)
                .serviceSpuId(6001L)
                .serviceSkuId(7001L)
                .overallScore(2)
                .serviceScore(2)
                .technicianScore(3)
                .environmentScore(2)
                .tags(Arrays.asList("服务敷衍", "沟通不清楚"))
                .content("本次体验没有达到预期")
                .picUrls(Arrays.asList("https://example.com/review-1.png"))
                .anonymous(Boolean.TRUE)
                .reviewLevel(3)
                .riskLevel(2)
                .displayStatus(0)
                .followStatus(1)
                .completedTime(LocalDateTime.now().minusHours(1).withNano(0))
                .submitTime(LocalDateTime.now().withNano(0))
                .build();

        bookingReviewMapper.insert(review);

        BookingReviewDO actual = bookingReviewMapper.selectByBookingOrderId(1001L);
        assertNotNull(actual);
        assertEquals(3001L, actual.getStoreId());
        assertEquals(review.getTags(), actual.getTags());
        assertEquals(review.getPicUrls(), actual.getPicUrls());
        assertEquals(2, actual.getOverallScore());
    }
}
