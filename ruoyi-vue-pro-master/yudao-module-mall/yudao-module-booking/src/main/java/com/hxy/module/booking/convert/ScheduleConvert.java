package com.hxy.module.booking.convert;

import com.hxy.module.booking.controller.admin.vo.ScheduleRespVO;
import com.hxy.module.booking.dal.dataobject.TechnicianScheduleDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ScheduleConvert {

    ScheduleConvert INSTANCE = Mappers.getMapper(ScheduleConvert.class);

    ScheduleRespVO convert(TechnicianScheduleDO bean);

    List<ScheduleRespVO> convertList(List<TechnicianScheduleDO> list);

}
