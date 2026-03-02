package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.dal.dataobject.TechnicianScheduleDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TechnicianScheduleMapper extends BaseMapperX<TechnicianScheduleDO> {

    default List<TechnicianScheduleDO> selectListByTechnicianIdAndDateRange(Long technicianId, LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<TechnicianScheduleDO>()
                .eq(TechnicianScheduleDO::getTechnicianId, technicianId)
                .ge(TechnicianScheduleDO::getScheduleDate, startDate)
                .le(TechnicianScheduleDO::getScheduleDate, endDate)
                .orderByAsc(TechnicianScheduleDO::getScheduleDate));
    }

    default List<TechnicianScheduleDO> selectListByStoreIdAndDate(Long storeId, LocalDate scheduleDate) {
        return selectList(new LambdaQueryWrapperX<TechnicianScheduleDO>()
                .eq(TechnicianScheduleDO::getStoreId, storeId)
                .eq(TechnicianScheduleDO::getScheduleDate, scheduleDate));
    }

    default TechnicianScheduleDO selectByTechnicianIdAndDate(Long technicianId, LocalDate scheduleDate) {
        return selectOne(new LambdaQueryWrapperX<TechnicianScheduleDO>()
                .eq(TechnicianScheduleDO::getTechnicianId, technicianId)
                .eq(TechnicianScheduleDO::getScheduleDate, scheduleDate));
    }

    default List<TechnicianScheduleDO> selectListByStoreIdAndDateRange(Long storeId, LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<TechnicianScheduleDO>()
                .eq(TechnicianScheduleDO::getStoreId, storeId)
                .ge(TechnicianScheduleDO::getScheduleDate, startDate)
                .le(TechnicianScheduleDO::getScheduleDate, endDate)
                .orderByAsc(TechnicianScheduleDO::getScheduleDate));
    }

}
