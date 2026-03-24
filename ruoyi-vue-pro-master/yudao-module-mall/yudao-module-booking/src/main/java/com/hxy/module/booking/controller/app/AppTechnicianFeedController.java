package com.hxy.module.booking.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedCommentCreateReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedCommentRespVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedLikeReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedLikeRespVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedPageReqVO;
import com.hxy.module.booking.controller.app.vo.feed.AppTechnicianFeedPageRespVO;
import com.hxy.module.booking.service.TechnicianFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.PermitAll;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户端 - 技师动态")
@RestController
@RequestMapping("/booking/technician/feed")
@Validated
@RequiredArgsConstructor
public class AppTechnicianFeedController {

    private final TechnicianFeedService technicianFeedService;

    @GetMapping("/page")
    @PermitAll
    @Operation(summary = "分页获取技师动态")
    public CommonResult<AppTechnicianFeedPageRespVO> getFeedPage(@Valid AppTechnicianFeedPageReqVO reqVO) {
        return success(technicianFeedService.getFeedPage(reqVO));
    }

    @PostMapping("/like")
    @Operation(summary = "点赞或取消点赞技师动态")
    public CommonResult<AppTechnicianFeedLikeRespVO> toggleLike(@Valid @RequestBody AppTechnicianFeedLikeReqVO reqVO) {
        return success(technicianFeedService.toggleLike(getLoginUserId(), reqVO));
    }

    @PostMapping("/comment/create")
    @Operation(summary = "发表评论")
    public CommonResult<AppTechnicianFeedCommentRespVO> createComment(
            @Valid @RequestBody AppTechnicianFeedCommentCreateReqVO reqVO) {
        return success(technicianFeedService.createComment(getLoginUserId(), reqVO));
    }
}
