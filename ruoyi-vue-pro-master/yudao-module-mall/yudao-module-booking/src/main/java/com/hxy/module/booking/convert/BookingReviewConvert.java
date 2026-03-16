package com.hxy.module.booking.convert;

import com.hxy.module.booking.controller.app.vo.AppBookingReviewRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface BookingReviewConvert {

    BookingReviewConvert INSTANCE = Mappers.getMapper(BookingReviewConvert.class);

    AppBookingReviewRespVO convert(BookingReviewDO bean);

    List<AppBookingReviewRespVO> convertList(List<BookingReviewDO> list);
}
