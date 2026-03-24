package com.hxy.module.booking.dal.mysql.feed;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.dal.dataobject.feed.TechnicianFeedLikeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TechnicianFeedLikeMapper extends BaseMapperX<TechnicianFeedLikeDO> {

    default TechnicianFeedLikeDO selectActiveByPostIdAndMemberId(Long postId, Long memberId) {
        return selectOne(new LambdaQueryWrapperX<TechnicianFeedLikeDO>()
                .eq(TechnicianFeedLikeDO::getPostId, postId)
                .eq(TechnicianFeedLikeDO::getMemberId, memberId)
                .eq(TechnicianFeedLikeDO::getStatus, 1)
                .orderByDesc(TechnicianFeedLikeDO::getId)
                .last("LIMIT 1"));
    }
}
