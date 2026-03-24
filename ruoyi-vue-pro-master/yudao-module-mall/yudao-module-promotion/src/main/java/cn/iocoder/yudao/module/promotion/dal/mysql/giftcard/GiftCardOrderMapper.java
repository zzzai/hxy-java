package cn.iocoder.yudao.module.promotion.dal.mysql.giftcard;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.promotion.dal.dataobject.giftcard.GiftCardOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GiftCardOrderMapper extends BaseMapperX<GiftCardOrderDO> {

    default GiftCardOrderDO selectByIdAndMemberId(Long id, Long memberId) {
        return selectOne(new LambdaQueryWrapperX<GiftCardOrderDO>()
                .eq(GiftCardOrderDO::getId, id)
                .eq(GiftCardOrderDO::getMemberId, memberId)
                .last("LIMIT 1"));
    }
}
