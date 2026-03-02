package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface TechnicianCommissionSettlementLogMapper extends BaseMapperX<TechnicianCommissionSettlementLogDO> {

    default List<TechnicianCommissionSettlementLogDO> selectListBySettlementId(Long settlementId) {
        return selectList(new LambdaQueryWrapperX<TechnicianCommissionSettlementLogDO>()
                .eq(TechnicianCommissionSettlementLogDO::getSettlementId, settlementId)
                .orderByAsc(TechnicianCommissionSettlementLogDO::getId));
    }

    default List<TechnicianCommissionSettlementLogDO> selectLatestListBySettlementIds(Collection<Long> settlementIds) {
        if (settlementIds == null || settlementIds.isEmpty()) {
            return List.of();
        }
        List<TechnicianCommissionSettlementLogDO> orderedLogs = selectList(new LambdaQueryWrapperX<TechnicianCommissionSettlementLogDO>()
                .in(TechnicianCommissionSettlementLogDO::getSettlementId, settlementIds)
                .orderByDesc(TechnicianCommissionSettlementLogDO::getId));
        Map<Long, TechnicianCommissionSettlementLogDO> latestMap = new LinkedHashMap<>();
        for (TechnicianCommissionSettlementLogDO log : orderedLogs) {
            if (log == null || log.getSettlementId() == null) {
                continue;
            }
            latestMap.putIfAbsent(log.getSettlementId(), log);
        }
        return new ArrayList<>(latestMap.values());
    }
}
