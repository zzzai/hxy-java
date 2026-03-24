package com.hxy.module.booking.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedCommentCreateReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedCommentRespVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedLikeReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedLikeRespVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedPageReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedPageRespVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedPostRespVO;
import com.hxy.module.booking.service.TechnicianFeedService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppTechnicianFeedControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppTechnicianFeedController controller;

    @Mock
    private TechnicianFeedService technicianFeedService;

    @Test
    void shouldGetFeedPage() {
        AppTechnicianFeedPageReqVO reqVO = new AppTechnicianFeedPageReqVO();
        reqVO.setStoreId(9L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        AppTechnicianFeedPostRespVO post = new AppTechnicianFeedPostRespVO()
                .setPostId(1001L)
                .setTechnicianId(88L)
                .setContent("今天状态不错");
        AppTechnicianFeedPageRespVO respVO = new AppTechnicianFeedPageRespVO()
                .setList(Collections.singletonList(post))
                .setHasMore(false);
        when(technicianFeedService.getFeedPage(reqVO)).thenReturn(respVO);

        CommonResult<AppTechnicianFeedPageRespVO> result = controller.getFeedPage(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getList().size());
        assertEquals(1001L, result.getData().getList().get(0).getPostId());
        verify(technicianFeedService).getFeedPage(reqVO);
    }

    @Test
    void shouldToggleLike() {
        AppTechnicianFeedLikeReqVO reqVO = new AppTechnicianFeedLikeReqVO();
        reqVO.setPostId(1002L);
        reqVO.setAction(1);
        reqVO.setClientToken("like-001");
        AppTechnicianFeedLikeRespVO respVO = new AppTechnicianFeedLikeRespVO()
                .setPostId(1002L)
                .setLiked(true)
                .setLikeCount(4)
                .setIdempotentHit(false);
        when(technicianFeedService.toggleLike(66L, reqVO)).thenReturn(respVO);

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);

            CommonResult<AppTechnicianFeedLikeRespVO> result = controller.toggleLike(reqVO);

            assertTrue(result.isSuccess());
            assertEquals(4, result.getData().getLikeCount());
        }

        verify(technicianFeedService).toggleLike(66L, reqVO);
    }

    @Test
    void shouldCreateComment() {
        AppTechnicianFeedCommentCreateReqVO reqVO = new AppTechnicianFeedCommentCreateReqVO();
        reqVO.setPostId(1003L);
        reqVO.setContent("预约体验很好");
        reqVO.setClientToken("comment-001");
        AppTechnicianFeedCommentRespVO respVO = new AppTechnicianFeedCommentRespVO()
                .setCommentId(7001L)
                .setPostId(1003L)
                .setStatus("REVIEWING")
                .setDegraded(false);
        when(technicianFeedService.createComment(66L, reqVO)).thenReturn(respVO);

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);

            CommonResult<AppTechnicianFeedCommentRespVO> result = controller.createComment(reqVO);

            assertTrue(result.isSuccess());
            assertEquals("REVIEWING", result.getData().getStatus());
        }

        verify(technicianFeedService).createComment(66L, reqVO);
    }
}
