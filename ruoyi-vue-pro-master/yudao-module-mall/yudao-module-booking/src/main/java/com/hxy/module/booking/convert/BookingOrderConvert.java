package com.hxy.module.booking.convert;

import com.hxy.module.booking.controller.app.vo.AppBookingOrderRespVO;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface BookingOrderConvert {

    BookingOrderConvert INSTANCE = Mappers.getMapper(BookingOrderConvert.class);

    AppBookingOrderRespVO convert(BookingOrderDO bean);

    List<AppBookingOrderRespVO> convertList(List<BookingOrderDO> list);

}
