package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TechnicianCommissionSettlementLogMapper extends BaseMapperX<TechnicianCommissionSettlementLogDO> {

    default List<TechnicianCommissionSettlementLogDO> selectListBySettlementId(Long settlementId) {
        return selectList(new LambdaQueryWrapperX<TechnicianCommissionSettlementLogDO>()
                .eq(TechnicianCommissionSettlementLogDO::getSettlementId, settlementId)
                .orderByAsc(TechnicianCommissionSettlementLogDO::getId));
    }
}

