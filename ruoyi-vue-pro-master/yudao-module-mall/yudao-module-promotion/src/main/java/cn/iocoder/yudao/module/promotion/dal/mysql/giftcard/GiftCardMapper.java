package cn.iocoder.yudao.module.promotion.dal.mysql.giftcard;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.promotion.dal.dataobject.giftcard.GiftCardDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GiftCardMapper extends BaseMapperX<GiftCardDO> {

    default List<GiftCardDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<GiftCardDO>()
                .eq(GiftCardDO::getOrderId, orderId)
                .orderByAsc(GiftCardDO::getId));
    }

    default GiftCardDO selectByCardNo(String cardNo) {
        return selectOne(new LambdaQueryWrapperX<GiftCardDO>()
                .eq(GiftCardDO::getCardNo, cardNo)
                .last("LIMIT 1"));
    }

    @Update("UPDATE gift_card SET status = #{status} WHERE order_id = #{orderId}")
    int markOrderCardsRefundPending(@Param("orderId") Long orderId, @Param("status") String status);
}
