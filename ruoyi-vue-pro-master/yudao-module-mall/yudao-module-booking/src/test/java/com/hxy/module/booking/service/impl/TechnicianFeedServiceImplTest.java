package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedCommentCreateReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedCommentRespVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedLikeReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedLikeRespVO;
import com.hxy.module.booking.dal.dataobject.TechnicianDO;
import com.hxy.module.booking.dal.dataobject.feed.TechnicianFeedCommentDO;
import com.hxy.module.booking.dal.dataobject.feed.TechnicianFeedLikeDO;
import com.hxy.module.booking.dal.dataobject.feed.TechnicianFeedPostDO;
import com.hxy.module.booking.dal.mysql.feed.TechnicianFeedCommentMapper;
import com.hxy.module.booking.dal.mysql.feed.TechnicianFeedLikeMapper;
import com.hxy.module.booking.dal.mysql.feed.TechnicianFeedPostMapper;
import com.hxy.module.booking.service.TechnicianService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicianFeedServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TechnicianFeedServiceImpl service;

    @Mock
    private TechnicianFeedPostMapper technicianFeedPostMapper;
    @Mock
    private TechnicianFeedLikeMapper technicianFeedLikeMapper;
    @Mock
    private TechnicianFeedCommentMapper technicianFeedCommentMapper;
    @Mock
    private TechnicianService technicianService;

    @Test
    void shouldToggleLike() {
        AppTechnicianFeedLikeReqVO reqVO = new AppTechnicianFeedLikeReqVO();
        reqVO.setPostId(1001L);
        reqVO.setAction(1);
        reqVO.setClientToken("like-001");
        TechnicianFeedPostDO post = TechnicianFeedPostDO.builder()
                .id(1001L)
                .technicianId(88L)
                .storeId(9L)
                .likeCount(3)
                .status(1)
                .publishedAt(LocalDateTime.now())
                .build();
        when(technicianFeedPostMapper.selectById(1001L)).thenReturn(post);
        when(technicianFeedLikeMapper.selectActiveByPostIdAndMemberId(1001L, 66L)).thenReturn(null);

        AppTechnicianFeedLikeRespVO result = service.toggleLike(66L, reqVO);

        assertTrue(result.getLiked());
        assertEquals(4, result.getLikeCount());
        assertFalse(result.getIdempotentHit());
        verify(technicianFeedLikeMapper).insert(any(TechnicianFeedLikeDO.class));
        verify(technicianFeedPostMapper).updateLikeCount(1001L, 1);
    }

    @Test
    void shouldCreateComment() {
        AppTechnicianFeedCommentCreateReqVO reqVO = new AppTechnicianFeedCommentCreateReqVO();
        reqVO.setPostId(1002L);
        reqVO.setContent("很专业");
        reqVO.setClientToken("comment-001");
        TechnicianFeedPostDO post = TechnicianFeedPostDO.builder()
                .id(1002L)
                .technicianId(88L)
                .storeId(9L)
                .commentCount(2)
                .status(1)
                .publishedAt(LocalDateTime.now())
                .build();
        TechnicianDO technician = TechnicianDO.builder().id(88L).status(0).build();
        when(technicianFeedPostMapper.selectById(1002L)).thenReturn(post);
        when(technicianService.getTechnician(88L)).thenReturn(technician);

        AppTechnicianFeedCommentRespVO result = service.createComment(66L, reqVO);

        assertEquals(1002L, result.getPostId());
        assertEquals("REVIEWING", result.getStatus());
        assertFalse(result.getDegraded());
        verify(technicianFeedCommentMapper).insert(any(TechnicianFeedCommentDO.class));
        verify(technicianFeedPostMapper).updateCommentCount(1002L, 1);
    }
}
