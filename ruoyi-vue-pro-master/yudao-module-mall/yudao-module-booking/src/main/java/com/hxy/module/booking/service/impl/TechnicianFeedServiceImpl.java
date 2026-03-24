package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedCommentCreateReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedCommentRespVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedLikeReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedLikeRespVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedPageReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedPageRespVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedPostRespVO;
import com.hxy.module.booking.dal.dataobject.TechnicianDO;
import com.hxy.module.booking.dal.dataobject.feed.TechnicianFeedCommentDO;
import com.hxy.module.booking.dal.dataobject.feed.TechnicianFeedLikeDO;
import com.hxy.module.booking.dal.dataobject.feed.TechnicianFeedPostDO;
import com.hxy.module.booking.dal.mysql.feed.TechnicianFeedCommentMapper;
import com.hxy.module.booking.dal.mysql.feed.TechnicianFeedLikeMapper;
import com.hxy.module.booking.dal.mysql.feed.TechnicianFeedPostMapper;
import com.hxy.module.booking.service.TechnicianFeedService;
import com.hxy.module.booking.service.TechnicianService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.TECHNICIAN_DISABLED;
import static com.hxy.module.booking.enums.ErrorCodeConstants.TECHNICIAN_FEED_POST_NOT_EXISTS;
import static com.hxy.module.booking.enums.ErrorCodeConstants.TECHNICIAN_NOT_EXISTS;

@Service
@Validated
@RequiredArgsConstructor
public class TechnicianFeedServiceImpl implements TechnicianFeedService {

    private static final int POST_STATUS_PUBLISHED = 1;
    private static final int LIKE_ACTION_CANCEL = 0;
    private static final int LIKE_ACTION_LIKE = 1;
    private static final int LIKE_STATUS_ACTIVE = 1;
    private static final int LIKE_STATUS_CANCELED = 0;
    private static final String COMMENT_STATUS_REVIEWING = "REVIEWING";

    private final TechnicianFeedPostMapper technicianFeedPostMapper;
    private final TechnicianFeedLikeMapper technicianFeedLikeMapper;
    private final TechnicianFeedCommentMapper technicianFeedCommentMapper;
    private final TechnicianService technicianService;

    @Override
    public AppTechnicianFeedPageRespVO getFeedPage(AppTechnicianFeedPageReqVO reqVO) {
        List<TechnicianFeedPostDO> rows = technicianFeedPostMapper.selectAppFeedPage(
                reqVO.getStoreId(), reqVO.getTechnicianId(), reqVO.getLastId(), reqVO.getPageSize());
        if (rows == null) {
            rows = Collections.emptyList();
        }
        int pageSize = reqVO.getPageSize() == null || reqVO.getPageSize() <= 0 ? 10 : Math.min(reqVO.getPageSize(), 20);
        boolean hasMore = rows.size() > pageSize;
        List<TechnicianFeedPostDO> pageRows = hasMore ? rows.subList(0, pageSize) : rows;
        Long nextCursor = hasMore && !pageRows.isEmpty() ? pageRows.get(pageRows.size() - 1).getId() : null;
        return new AppTechnicianFeedPageRespVO()
                .setList(pageRows.stream().map(this::convertPost).collect(Collectors.toList()))
                .setHasMore(hasMore)
                .setNextCursor(nextCursor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppTechnicianFeedLikeRespVO toggleLike(Long memberId, AppTechnicianFeedLikeReqVO reqVO) {
        TechnicianFeedPostDO post = validatePost(reqVO.getPostId());
        TechnicianFeedLikeDO activeLike = technicianFeedLikeMapper.selectActiveByPostIdAndMemberId(reqVO.getPostId(), memberId);
        boolean likeAction = LIKE_ACTION_LIKE == reqVO.getAction();
        int currentLikeCount = safeCount(post.getLikeCount());
        if (likeAction) {
            if (activeLike != null) {
                return new AppTechnicianFeedLikeRespVO()
                        .setPostId(post.getId())
                        .setLiked(Boolean.TRUE)
                        .setLikeCount(currentLikeCount)
                        .setIdempotentHit(Boolean.TRUE);
            }
            TechnicianFeedLikeDO likeDO = TechnicianFeedLikeDO.builder()
                    .postId(post.getId())
                    .memberId(memberId)
                    .clientToken(reqVO.getClientToken().trim())
                    .status(LIKE_STATUS_ACTIVE)
                    .likedAt(LocalDateTime.now().withNano(0))
                    .build();
            technicianFeedLikeMapper.insert(likeDO);
            technicianFeedPostMapper.updateLikeCount(post.getId(), 1);
            return new AppTechnicianFeedLikeRespVO()
                    .setPostId(post.getId())
                    .setLiked(Boolean.TRUE)
                    .setLikeCount(currentLikeCount + 1)
                    .setIdempotentHit(Boolean.FALSE);
        }
        if (activeLike == null) {
            return new AppTechnicianFeedLikeRespVO()
                    .setPostId(post.getId())
                    .setLiked(Boolean.FALSE)
                    .setLikeCount(currentLikeCount)
                    .setIdempotentHit(Boolean.TRUE);
        }
        activeLike.setStatus(LIKE_STATUS_CANCELED);
        activeLike.setCanceledAt(LocalDateTime.now().withNano(0));
        technicianFeedLikeMapper.updateById(activeLike);
        technicianFeedPostMapper.updateLikeCount(post.getId(), -1);
        return new AppTechnicianFeedLikeRespVO()
                .setPostId(post.getId())
                .setLiked(Boolean.FALSE)
                .setLikeCount(Math.max(currentLikeCount - 1, 0))
                .setIdempotentHit(Boolean.FALSE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppTechnicianFeedCommentRespVO createComment(Long memberId, AppTechnicianFeedCommentCreateReqVO reqVO) {
        TechnicianFeedPostDO post = validatePost(reqVO.getPostId());
        TechnicianDO technician = technicianService.getTechnician(post.getTechnicianId());
        if (technician == null) {
            throw exception(TECHNICIAN_NOT_EXISTS);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(technician.getStatus())) {
            throw exception(TECHNICIAN_DISABLED);
        }
        TechnicianFeedCommentDO commentDO = TechnicianFeedCommentDO.builder()
                .postId(post.getId())
                .storeId(post.getStoreId())
                .technicianId(post.getTechnicianId())
                .memberId(memberId)
                .content(reqVO.getContent().trim())
                .clientToken(reqVO.getClientToken().trim())
                .status(COMMENT_STATUS_REVIEWING)
                .degraded(Boolean.FALSE)
                .submittedAt(LocalDateTime.now().withNano(0))
                .build();
        technicianFeedCommentMapper.insert(commentDO);
        technicianFeedPostMapper.updateCommentCount(post.getId(), 1);
        return new AppTechnicianFeedCommentRespVO()
                .setCommentId(commentDO.getId())
                .setPostId(post.getId())
                .setStatus(COMMENT_STATUS_REVIEWING)
                .setDegraded(Boolean.FALSE);
    }

    private TechnicianFeedPostDO validatePost(Long postId) {
        TechnicianFeedPostDO post = technicianFeedPostMapper.selectById(postId);
        if (post == null || !Integer.valueOf(POST_STATUS_PUBLISHED).equals(post.getStatus())) {
            throw exception(TECHNICIAN_FEED_POST_NOT_EXISTS);
        }
        return post;
    }

    private AppTechnicianFeedPostRespVO convertPost(TechnicianFeedPostDO post) {
        return new AppTechnicianFeedPostRespVO()
                .setPostId(post.getId())
                .setStoreId(post.getStoreId())
                .setTechnicianId(post.getTechnicianId())
                .setTitle(post.getTitle())
                .setContent(post.getContent())
                .setCoverUrl(post.getCoverUrl())
                .setLikeCount(safeCount(post.getLikeCount()))
                .setCommentCount(safeCount(post.getCommentCount()))
                .setPublishedAt(post.getPublishedAt());
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : Math.max(count, 0);
    }
}
