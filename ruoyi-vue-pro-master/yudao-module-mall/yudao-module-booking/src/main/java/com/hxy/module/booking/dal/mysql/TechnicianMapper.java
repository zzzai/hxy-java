package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.dal.dataobject.TechnicianDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TechnicianMapper extends BaseMapperX<TechnicianDO> {

    default List<TechnicianDO> selectListByStoreId(Long storeId) {
        return selectList(new LambdaQueryWrapperX<TechnicianDO>()
                .eq(TechnicianDO::getStoreId, storeId)
                .orderByAsc(TechnicianDO::getSort));
    }

    default List<TechnicianDO> selectListByStoreIdAndStatus(Long storeId, Integer status) {
        return selectList(new LambdaQueryWrapperX<TechnicianDO>()
                .eq(TechnicianDO::getStoreId, storeId)
                .eq(TechnicianDO::getStatus, status)
                .orderByAsc(TechnicianDO::getSort));
    }

    default TechnicianDO selectByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapperX<TechnicianDO>()
                .eq(TechnicianDO::getUserId, userId));
    }

}
