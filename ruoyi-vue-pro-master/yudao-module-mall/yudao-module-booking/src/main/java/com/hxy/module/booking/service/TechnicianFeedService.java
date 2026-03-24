package com.hxy.module.booking.service;

import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedCommentCreateReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedCommentRespVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedLikeReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedLikeRespVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedPageReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedPageRespVO;

public interface TechnicianFeedService {

    AppTechnicianFeedPageRespVO getFeedPage(AppTechnicianFeedPageReqVO reqVO);

    AppTechnicianFeedLikeRespVO toggleLike(Long memberId, AppTechnicianFeedLikeReqVO reqVO);

    AppTechnicianFeedCommentRespVO createComment(Long memberId, AppTechnicianFeedCommentCreateReqVO reqVO);
}
