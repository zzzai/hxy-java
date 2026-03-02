package com.hxy.module.booking.convert;

import com.hxy.module.booking.controller.admin.vo.OffpeakRuleCreateReqVO;
import com.hxy.module.booking.controller.admin.vo.OffpeakRuleRespVO;
import com.hxy.module.booking.controller.admin.vo.OffpeakRuleUpdateReqVO;
import com.hxy.module.booking.dal.dataobject.OffpeakRuleDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface OffpeakRuleConvert {

    OffpeakRuleConvert INSTANCE = Mappers.getMapper(OffpeakRuleConvert.class);

    OffpeakRuleDO convert(OffpeakRuleCreateReqVO bean);

    OffpeakRuleDO convert(OffpeakRuleUpdateReqVO bean);

    OffpeakRuleRespVO convert(OffpeakRuleDO bean);

    List<OffpeakRuleRespVO> convertList(List<OffpeakRuleDO> list);

}
