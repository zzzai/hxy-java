package com.hxy.module.booking.dal.mysql.feed;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.dal.dataobject.feed.TechnicianFeedPostDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TechnicianFeedPostMapper extends BaseMapperX<TechnicianFeedPostDO> {

    default List<TechnicianFeedPostDO> selectAppFeedPage(Long storeId, Long technicianId, Long lastId, Integer pageSize) {
        int safePageSize = pageSize == null || pageSize <= 0 ? 10 : Math.min(pageSize, 20);
        return selectList(new LambdaQueryWrapperX<TechnicianFeedPostDO>()
                .eq(TechnicianFeedPostDO::getStoreId, storeId)
                .eqIfPresent(TechnicianFeedPostDO::getTechnicianId, technicianId)
                .eq(TechnicianFeedPostDO::getStatus, 1)
                .ltIfPresent(TechnicianFeedPostDO::getId, lastId)
                .orderByDesc(TechnicianFeedPostDO::getPublishedAt, TechnicianFeedPostDO::getId)
                .last("LIMIT " + (safePageSize + 1)));
    }

    @Update("UPDATE technician_feed_post SET like_count = GREATEST(COALESCE(like_count, 0) + #{delta}, 0) WHERE id = #{postId}")
    int updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Update("UPDATE technician_feed_post SET comment_count = GREATEST(COALESCE(comment_count, 0) + #{delta}, 0) WHERE id = #{postId}")
    int updateCommentCount(@Param("postId") Long postId, @Param("delta") int delta);
}
