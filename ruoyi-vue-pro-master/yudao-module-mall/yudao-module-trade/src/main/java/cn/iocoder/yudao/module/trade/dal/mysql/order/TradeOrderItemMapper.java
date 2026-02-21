package cn.iocoder.yudao.module.trade.dal.mysql.order;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper
public interface TradeOrderItemMapper extends BaseMapperX<TradeOrderItemDO> {

    default int updateAfterSaleStatus(Long id, Integer oldAfterSaleStatus, Integer newAfterSaleStatus,
                                      Long afterSaleId) {
        return update(new TradeOrderItemDO().setAfterSaleStatus(newAfterSaleStatus).setAfterSaleId(afterSaleId),
                new LambdaUpdateWrapper<>(new TradeOrderItemDO().setId(id).setAfterSaleStatus(oldAfterSaleStatus)));
    }

    default List<TradeOrderItemDO> selectListByOrderId(Long orderId) {
        return selectList(TradeOrderItemDO::getOrderId, orderId);
    }

    default List<TradeOrderItemDO> selectListByOrderId(Collection<Long> orderIds) {
        return selectList(TradeOrderItemDO::getOrderId, orderIds);
    }

    default TradeOrderItemDO selectByIdAndUserId(Long orderItemId, Long loginUserId) {
        return selectOne(new LambdaQueryWrapperX<TradeOrderItemDO>()
                .eq(TradeOrderItemDO::getId, orderItemId)
                .eq(TradeOrderItemDO::getUserId, loginUserId));
    }

    default List<TradeOrderItemDO> selectListByOrderIdAndCommentStatus(Long orderId, Boolean commentStatus) {
        return selectList(new LambdaQueryWrapperX<TradeOrderItemDO>()
                .eq(TradeOrderItemDO::getOrderId, orderId)
                .eq(TradeOrderItemDO::getCommentStatus, commentStatus));
    }

    default int selectProductSumByOrderId(@Param("orderIds") Set<Long> orderIds) {
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<TradeOrderItemDO>()
                .select("SUM(count) AS sumCount")
                .in("order_id", orderIds)); // 只计算选中的
        // 获得数量
        return CollUtil.getFirst(result) != null ? MapUtil.getInt(result.get(0), "sumCount") : 0;
    }

    default Set<Long> selectOrderIdsByUserIdAndAfterSaleStatuses(Long userId, Collection<Integer> afterSaleStatuses) {
        if (CollUtil.isEmpty(afterSaleStatuses)) {
            return Collections.emptySet();
        }
        List<TradeOrderItemDO> orderItems = selectList(new LambdaQueryWrapperX<TradeOrderItemDO>()
                .select(TradeOrderItemDO::getOrderId)
                .eq(TradeOrderItemDO::getUserId, userId)
                .in(TradeOrderItemDO::getAfterSaleStatus, afterSaleStatuses));
        if (CollUtil.isEmpty(orderItems)) {
            return Collections.emptySet();
        }
        return orderItems.stream()
                .map(TradeOrderItemDO::getOrderId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
    }

    default Set<Long> selectOrderIdsByAfterSaleStatuses(Collection<Integer> afterSaleStatuses) {
        if (CollUtil.isEmpty(afterSaleStatuses)) {
            return Collections.emptySet();
        }
        List<TradeOrderItemDO> orderItems = selectList(new LambdaQueryWrapperX<TradeOrderItemDO>()
                .select(TradeOrderItemDO::getOrderId)
                .in(TradeOrderItemDO::getAfterSaleStatus, afterSaleStatuses));
        if (CollUtil.isEmpty(orderItems)) {
            return Collections.emptySet();
        }
        return orderItems.stream()
                .map(TradeOrderItemDO::getOrderId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
    }

}
