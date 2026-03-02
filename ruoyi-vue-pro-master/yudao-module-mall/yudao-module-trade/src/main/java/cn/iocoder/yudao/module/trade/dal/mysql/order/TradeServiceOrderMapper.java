package cn.iocoder.yudao.module.trade.dal.mysql.order;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder.TradeServiceOrderPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TradeServiceOrderMapper extends BaseMapperX<TradeServiceOrderDO> {

    default PageResult<TradeServiceOrderDO> selectPage(TradeServiceOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TradeServiceOrderDO>()
                .eqIfPresent(TradeServiceOrderDO::getOrderId, reqVO.getOrderId())
                .likeIfPresent(TradeServiceOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(TradeServiceOrderDO::getOrderItemId, reqVO.getOrderItemId())
                .eqIfPresent(TradeServiceOrderDO::getUserId, reqVO.getUserId())
                .eqIfPresent(TradeServiceOrderDO::getPayOrderId, reqVO.getPayOrderId())
                .eqIfPresent(TradeServiceOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(TradeServiceOrderDO::getSource, reqVO.getSource())
                .likeIfPresent(TradeServiceOrderDO::getBookingNo, reqVO.getBookingNo())
                .betweenIfPresent(TradeServiceOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TradeServiceOrderDO::getId));
    }

    default TradeServiceOrderDO selectByOrderItemId(Long orderItemId) {
        return selectOne(new LambdaQueryWrapperX<TradeServiceOrderDO>()
                .eq(TradeServiceOrderDO::getOrderItemId, orderItemId));
    }

    default List<TradeServiceOrderDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<TradeServiceOrderDO>()
                .eq(TradeServiceOrderDO::getOrderId, orderId)
                .orderByAsc(TradeServiceOrderDO::getId));
    }

    default int updateByIdAndStatus(Long id, Integer status, TradeServiceOrderDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<TradeServiceOrderDO>()
                .eq(TradeServiceOrderDO::getId, id)
                .eq(TradeServiceOrderDO::getStatus, status));
    }

    default List<TradeServiceOrderDO> selectListForBookingPlaceholderRetry(Integer status, Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, 200), 1000));
        return selectList(new LambdaQueryWrapperX<TradeServiceOrderDO>()
                .eq(TradeServiceOrderDO::getStatus, status)
                .and(wrapper -> wrapper.isNull(TradeServiceOrderDO::getBookingNo)
                        .or()
                        .eq(TradeServiceOrderDO::getBookingNo, ""))
                .orderByAsc(TradeServiceOrderDO::getId)
                .last("LIMIT " + safeLimit));
    }

}
