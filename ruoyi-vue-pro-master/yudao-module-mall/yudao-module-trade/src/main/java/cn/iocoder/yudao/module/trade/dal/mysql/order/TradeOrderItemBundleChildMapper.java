package cn.iocoder.yudao.module.trade.dal.mysql.order;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemBundleChildDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TradeOrderItemBundleChildMapper extends BaseMapperX<TradeOrderItemBundleChildDO> {

    default List<TradeOrderItemBundleChildDO> selectListByOrderItemId(Long orderItemId) {
        return selectList(new LambdaQueryWrapperX<TradeOrderItemBundleChildDO>()
                .eq(TradeOrderItemBundleChildDO::getOrderItemId, orderItemId)
                .orderByAsc(TradeOrderItemBundleChildDO::getId));
    }

    default boolean existsByOrderItemId(Long orderItemId) {
        return selectCount(new LambdaQueryWrapperX<TradeOrderItemBundleChildDO>()
                .eq(TradeOrderItemBundleChildDO::getOrderItemId, orderItemId)) > 0;
    }
}
