package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TechnicianCommissionConfigMapper extends BaseMapperX<TechnicianCommissionConfigDO> {

    default List<TechnicianCommissionConfigDO> selectListByStoreId(Long storeId) {
        return selectList(new LambdaQueryWrapperX<TechnicianCommissionConfigDO>()
                .eq(TechnicianCommissionConfigDO::getStoreId, storeId));
    }

    default TechnicianCommissionConfigDO selectByStoreIdAndType(Long storeId, Integer commissionType) {
        return selectOne(new LambdaQueryWrapperX<TechnicianCommissionConfigDO>()
                .eq(TechnicianCommissionConfigDO::getStoreId, storeId)
                .eq(TechnicianCommissionConfigDO::getCommissionType, commissionType));
    }

}
