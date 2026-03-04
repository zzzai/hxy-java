package cn.iocoder.yudao.module.trade.dal.mysql.aftersale;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketNotifyOutboxPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketNotifyOutboxDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后人工复核工单通知出站 Mapper
 */
@Mapper
public interface AfterSaleReviewTicketNotifyOutboxMapper extends BaseMapperX<AfterSaleReviewTicketNotifyOutboxDO> {

    default PageResult<AfterSaleReviewTicketNotifyOutboxDO> selectPage(AfterSaleReviewTicketNotifyOutboxPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AfterSaleReviewTicketNotifyOutboxDO>()
                .eqIfPresent(AfterSaleReviewTicketNotifyOutboxDO::getTicketId, reqVO.getTicketId())
                .eqIfPresent(AfterSaleReviewTicketNotifyOutboxDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AfterSaleReviewTicketNotifyOutboxDO::getNotifyType, reqVO.getNotifyType())
                .eqIfPresent(AfterSaleReviewTicketNotifyOutboxDO::getChannel, reqVO.getChannel())
                .eqIfPresent(AfterSaleReviewTicketNotifyOutboxDO::getLastActionCode, reqVO.getLastActionCode())
                .eqIfPresent(AfterSaleReviewTicketNotifyOutboxDO::getLastActionBizNo, reqVO.getLastActionBizNo())
                .betweenIfPresent(AfterSaleReviewTicketNotifyOutboxDO::getLastActionTime, reqVO.getLastActionTime())
                .orderByDesc(AfterSaleReviewTicketNotifyOutboxDO::getId));
    }

    default AfterSaleReviewTicketNotifyOutboxDO selectByBizKey(String bizKey) {
        return selectOne(AfterSaleReviewTicketNotifyOutboxDO::getBizKey, bizKey);
    }

    default List<AfterSaleReviewTicketNotifyOutboxDO> selectDispatchableList(LocalDateTime now,
                                                                              Integer limit,
                                                                              Integer maxRetryCount) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, 200), 1000));
        int safeMaxRetry = Math.max(1, Math.min(ObjectUtil.defaultIfNull(maxRetryCount, 5), 20));
        LambdaQueryWrapperX<AfterSaleReviewTicketNotifyOutboxDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.and(wrapper -> wrapper.eq(AfterSaleReviewTicketNotifyOutboxDO::getStatus, 0)
                        .or(w -> w.eq(AfterSaleReviewTicketNotifyOutboxDO::getStatus, 2)
                                .lt(AfterSaleReviewTicketNotifyOutboxDO::getRetryCount, safeMaxRetry)))
                .and(wrapper -> wrapper.isNull(AfterSaleReviewTicketNotifyOutboxDO::getNextRetryTime)
                        .or()
                        .le(AfterSaleReviewTicketNotifyOutboxDO::getNextRetryTime, now))
                .orderByAsc(AfterSaleReviewTicketNotifyOutboxDO::getId)
                .last("LIMIT " + safeLimit);
        return selectList(queryWrapper);
    }

    default int updateByIdAndStatus(Long id, Integer status, AfterSaleReviewTicketNotifyOutboxDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<AfterSaleReviewTicketNotifyOutboxDO>()
                .eq(AfterSaleReviewTicketNotifyOutboxDO::getId, id)
                .eq(AfterSaleReviewTicketNotifyOutboxDO::getStatus, status));
    }
}
