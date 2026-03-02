package com.hxy.module.booking.convert;

import com.hxy.module.booking.controller.app.vo.AppTimeSlotRespVO;
import com.hxy.module.booking.dal.dataobject.TimeSlotDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TimeSlotConvert {

    TimeSlotConvert INSTANCE = Mappers.getMapper(TimeSlotConvert.class);

    AppTimeSlotRespVO convert(TimeSlotDO bean);

    List<AppTimeSlotRespVO> convertList(List<TimeSlotDO> list);

}
