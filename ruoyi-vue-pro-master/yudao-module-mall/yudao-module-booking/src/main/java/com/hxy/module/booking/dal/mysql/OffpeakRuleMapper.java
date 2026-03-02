package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.dal.dataobject.OffpeakRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OffpeakRuleMapper extends BaseMapperX<OffpeakRuleDO> {

    default List<OffpeakRuleDO> selectListByStoreId(Long storeId) {
        return selectList(new LambdaQueryWrapperX<OffpeakRuleDO>()
                .eq(OffpeakRuleDO::getStoreId, storeId));
    }

    default List<OffpeakRuleDO> selectEnabledByStoreId(Long storeId) {
        return selectList(new LambdaQueryWrapperX<OffpeakRuleDO>()
                .eq(OffpeakRuleDO::getStoreId, storeId)
                .eq(OffpeakRuleDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
    }

}
