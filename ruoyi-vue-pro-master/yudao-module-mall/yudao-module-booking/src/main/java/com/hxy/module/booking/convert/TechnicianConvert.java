package com.hxy.module.booking.convert;

import com.hxy.module.booking.controller.admin.vo.TechnicianCreateReqVO;
import com.hxy.module.booking.controller.admin.vo.TechnicianRespVO;
import com.hxy.module.booking.controller.admin.vo.TechnicianUpdateReqVO;
import com.hxy.module.booking.controller.app.vo.AppTechnicianRespVO;
import com.hxy.module.booking.dal.dataobject.TechnicianDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TechnicianConvert {

    TechnicianConvert INSTANCE = Mappers.getMapper(TechnicianConvert.class);

    TechnicianDO convert(TechnicianCreateReqVO bean);

    TechnicianDO convert(TechnicianUpdateReqVO bean);

    TechnicianRespVO convert(TechnicianDO bean);

    List<TechnicianRespVO> convertList(List<TechnicianDO> list);

    AppTechnicianRespVO convertApp(TechnicianDO bean);

    List<AppTechnicianRespVO> convertAppList(List<TechnicianDO> list);

}
