package com.hxy.module.booking.dal.mysql;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcilePageReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundAuditSummaryReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileSummaryReqVO;
import com.hxy.module.booking.dal.dataobject.FourAccountReconcileDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

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
                .eqIfPresent(FourAccountReconcileDO::getRefundAuditStatus, reqVO.getRefundAuditStatus())
                .eqIfPresent(FourAccountReconcileDO::getRefundExceptionType, reqVO.getRefundExceptionType())
                .eqIfPresent(FourAccountReconcileDO::getRefundLimitSource, reqVO.getRefundLimitSource())
                .eqIfPresent(FourAccountReconcileDO::getPayRefundId, reqVO.getPayRefundId())
                .betweenIfPresent(FourAccountReconcileDO::getRefundTime, reqVO.getRefundTimeRange())
                .orderByDesc(FourAccountReconcileDO::getBizDate)
                .orderByDesc(FourAccountReconcileDO::getId);
        if (StrUtil.isNotBlank(reqVO.getIssueCode())) {
            queryWrapper.like(FourAccountReconcileDO::getIssueCodes, reqVO.getIssueCode().trim().toUpperCase());
        }
        return selectPage(reqVO, queryWrapper);
    }

    default List<FourAccountReconcileDO> selectSummaryList(FourAccountReconcileSummaryReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<FourAccountReconcileDO>()
                .betweenIfPresent(FourAccountReconcileDO::getBizDate, reqVO.getBizDate())
                .eqIfPresent(FourAccountReconcileDO::getStatus, reqVO.getStatus())
                .orderByDesc(FourAccountReconcileDO::getBizDate)
                .orderByDesc(FourAccountReconcileDO::getId));
    }

    default List<FourAccountReconcileDO> selectRefundAuditSummaryList(FourAccountRefundAuditSummaryReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<FourAccountReconcileDO>()
                .betweenIfPresent(FourAccountReconcileDO::getBizDate, reqVO.getBizDate())
                .eqIfPresent(FourAccountReconcileDO::getStatus, reqVO.getStatus())
                .eqIfPresent(FourAccountReconcileDO::getRefundAuditStatus, reqVO.getRefundAuditStatus())
                .eqIfPresent(FourAccountReconcileDO::getRefundExceptionType, reqVO.getRefundExceptionType())
                .orderByDesc(FourAccountReconcileDO::getBizDate)
                .orderByDesc(FourAccountReconcileDO::getId));
    }
}
