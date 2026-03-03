package cn.iocoder.yudao.module.trade.dal.mysql.aftersale;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketDO;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后人工复核工单 Mapper
 *
 * @author HXY
 */
@Mapper
public interface AfterSaleReviewTicketMapper extends BaseMapperX<AfterSaleReviewTicketDO> {

    default PageResult<AfterSaleReviewTicketDO> selectPage(AfterSaleReviewTicketPageReqVO reqVO) {
        LambdaQueryWrapperX<AfterSaleReviewTicketDO> queryWrapper = new LambdaQueryWrapperX<AfterSaleReviewTicketDO>()
                .eqIfPresent(AfterSaleReviewTicketDO::getTicketType, reqVO.getTicketType())
                .eqIfPresent(AfterSaleReviewTicketDO::getAfterSaleId, reqVO.getAfterSaleId())
                .eqIfPresent(AfterSaleReviewTicketDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(AfterSaleReviewTicketDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AfterSaleReviewTicketDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AfterSaleReviewTicketDO::getSeverity, reqVO.getSeverity())
                .eqIfPresent(AfterSaleReviewTicketDO::getRuleCode, reqVO.getRuleCode())
                .eqIfPresent(AfterSaleReviewTicketDO::getSourceBizNo, reqVO.getSourceBizNo())
                .eqIfPresent(AfterSaleReviewTicketDO::getEscalateTo, reqVO.getEscalateTo())
                .eqIfPresent(AfterSaleReviewTicketDO::getRouteId, reqVO.getRouteId())
                .eqIfPresent(AfterSaleReviewTicketDO::getRouteScope, reqVO.getRouteScope())
                .eqIfPresent(AfterSaleReviewTicketDO::getLastActionCode, reqVO.getLastActionCode())
                .eqIfPresent(AfterSaleReviewTicketDO::getLastActionBizNo, reqVO.getLastActionBizNo())
                .betweenIfPresent(AfterSaleReviewTicketDO::getSlaDeadlineTime, reqVO.getSlaDeadlineTime())
                .betweenIfPresent(AfterSaleReviewTicketDO::getLastActionTime, reqVO.getLastActionTime())
                .betweenIfPresent(AfterSaleReviewTicketDO::getResolvedTime, reqVO.getResolvedTime())
                .betweenIfPresent(AfterSaleReviewTicketDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AfterSaleReviewTicketDO::getId);
        if (reqVO.getOverdue() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (Boolean.TRUE.equals(reqVO.getOverdue())) {
                queryWrapper.and(wrapper -> wrapper
                        .eq(AfterSaleReviewTicketDO::getStatus,
                                AfterSaleReviewTicketStatusEnum.PENDING.getStatus())
                        .isNotNull(AfterSaleReviewTicketDO::getSlaDeadlineTime)
                        .le(AfterSaleReviewTicketDO::getSlaDeadlineTime, now));
            } else {
                queryWrapper.and(wrapper -> wrapper
                        .ne(AfterSaleReviewTicketDO::getStatus,
                                AfterSaleReviewTicketStatusEnum.PENDING.getStatus())
                        .or()
                        .isNull(AfterSaleReviewTicketDO::getSlaDeadlineTime)
                        .or()
                        .gt(AfterSaleReviewTicketDO::getSlaDeadlineTime, now));
            }
        }
        return selectPage(reqVO, queryWrapper);
    }

    default AfterSaleReviewTicketDO selectByAfterSaleId(Long afterSaleId) {
        return selectOne(new LambdaQueryWrapperX<AfterSaleReviewTicketDO>()
                .eq(AfterSaleReviewTicketDO::getAfterSaleId, afterSaleId));
    }

    default int updateByIdAndStatus(Long id, Integer status, AfterSaleReviewTicketDO update) {
        return update(update, new LambdaUpdateWrapper<AfterSaleReviewTicketDO>()
                .eq(AfterSaleReviewTicketDO::getId, id)
                .eq(AfterSaleReviewTicketDO::getStatus, status));
    }

    default int updateByAfterSaleIdAndStatus(Long afterSaleId, Integer status, AfterSaleReviewTicketDO update) {
        return update(update, new LambdaUpdateWrapper<AfterSaleReviewTicketDO>()
                .eq(AfterSaleReviewTicketDO::getAfterSaleId, afterSaleId)
                .eq(AfterSaleReviewTicketDO::getStatus, status));
    }

    default List<AfterSaleReviewTicketDO> selectListByStatusAndSlaDeadlineTimeBefore(Integer status,
                                                                                      LocalDateTime deadlineTime,
                                                                                      Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, 200), 1000));
        return selectList(new LambdaQueryWrapperX<AfterSaleReviewTicketDO>()
                .eq(AfterSaleReviewTicketDO::getStatus, status)
                .isNotNull(AfterSaleReviewTicketDO::getSlaDeadlineTime)
                .le(AfterSaleReviewTicketDO::getSlaDeadlineTime, deadlineTime)
                .orderByAsc(AfterSaleReviewTicketDO::getSlaDeadlineTime)
                .last("LIMIT " + safeLimit));
    }

}
