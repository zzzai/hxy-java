package com.hxy.module.booking.dal.mysql;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcilePageReqVO;
import com.hxy.module.booking.dal.dataobject.FourAccountReconcileDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;

@Mapper
public interface FourAccountReconcileMapper extends BaseMapperX<FourAccountReconcileDO> {

    default FourAccountReconcileDO selectByBizDate(LocalDate bizDate) {
        return selectOne(new LambdaQueryWrapperX<FourAccountReconcileDO>()
                .eq(FourAccountReconcileDO::getBizDate, bizDate)
                .last("LIMIT 1"));
    }

    default PageResult<FourAccountReconcileDO> selectPage(FourAccountReconcilePageReqVO reqVO) {
        LambdaQueryWrapperX<FourAccountReconcileDO> queryWrapper = new LambdaQueryWrapperX<FourAccountReconcileDO>()
                .likeIfPresent(FourAccountReconcileDO::getReconcileNo, reqVO.getReconcileNo())
                .betweenIfPresent(FourAccountReconcileDO::getBizDate, reqVO.getBizDate())
                .eqIfPresent(FourAccountReconcileDO::getStatus, reqVO.getStatus())
                .eqIfPresent(FourAccountReconcileDO::getSource, reqVO.getSource())
                .orderByDesc(FourAccountReconcileDO::getBizDate)
                .orderByDesc(FourAccountReconcileDO::getId);
        if (StrUtil.isNotBlank(reqVO.getIssueCode())) {
            queryWrapper.like(FourAccountReconcileDO::getIssueCodes, reqVO.getIssueCode().trim().toUpperCase());
        }
        return selectPage(reqVO, queryWrapper);
    }
}

