package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import com.hxy.module.booking.controller.admin.vo.BookingReviewDashboardRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewFollowUpdateReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerTodoClaimReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerTodoCloseReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerTodoFirstActionReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewReplyReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.service.BookingReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 预约服务评价")
@RestController
@RequestMapping("/booking/review")
@Validated
public class BookingReviewController {

    @Resource
    private BookingReviewService bookingReviewService;

    @GetMapping("/page")
    @Operation(summary = "分页获得预约服务评价")
    @PreAuthorize("@ss.hasPermission('booking:review:query')")
    public CommonResult<PageResult<BookingReviewRespVO>> page(@Valid BookingReviewPageReqVO reqVO) {
        PageResult<BookingReviewDO> pageResult = bookingReviewService.getAdminReviewPage(reqVO);
        return success(BeanUtils.toBean(pageResult, BookingReviewRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得预约服务评价详情")
    @Parameter(name = "id", required = true, description = "评价ID")
    @PreAuthorize("@ss.hasPermission('booking:review:query')")
    public CommonResult<BookingReviewRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(bookingReviewService.getAdminReview(id), BookingReviewRespVO.class));
    }

    @PostMapping("/reply")
    @Operation(summary = "回复预约服务评价")
    @PreAuthorize("@ss.hasPermission('booking:review:update')")
    public CommonResult<Boolean> reply(@Valid @RequestBody BookingReviewReplyReqVO reqVO) {
        bookingReviewService.replyReview(reqVO.getReviewId(), getLoginUserId(), reqVO.getReplyContent());
        return success(true);
    }

    @PostMapping("/follow-status")
    @Operation(summary = "更新预约服务评价跟进状态")
    @PreAuthorize("@ss.hasPermission('booking:review:update')")
    public CommonResult<Boolean> updateFollowStatus(@Valid @RequestBody BookingReviewFollowUpdateReqVO reqVO) {
        bookingReviewService.updateFollowStatus(reqVO.getReviewId(), getLoginUserId(), reqVO);
        return success(true);
    }

    @PostMapping("/manager-todo/claim")
    @Operation(summary = "认领预约服务评价店长待办")
    @PreAuthorize("@ss.hasPermission('booking:review:update')")
    public CommonResult<Boolean> claimManagerTodo(@Valid @RequestBody BookingReviewManagerTodoClaimReqVO reqVO) {
        bookingReviewService.claimManagerTodo(reqVO.getReviewId(), getLoginUserId());
        return success(true);
    }

    @PostMapping("/manager-todo/first-action")
    @Operation(summary = "记录预约服务评价店长待办首次处理")
    @PreAuthorize("@ss.hasPermission('booking:review:update')")
    public CommonResult<Boolean> recordManagerFirstAction(
            @Valid @RequestBody BookingReviewManagerTodoFirstActionReqVO reqVO) {
        bookingReviewService.recordManagerFirstAction(reqVO.getReviewId(), getLoginUserId(), reqVO.getRemark());
        return success(true);
    }

    @PostMapping("/manager-todo/close")
    @Operation(summary = "关闭预约服务评价店长待办")
    @PreAuthorize("@ss.hasPermission('booking:review:update')")
    public CommonResult<Boolean> closeManagerTodo(@Valid @RequestBody BookingReviewManagerTodoCloseReqVO reqVO) {
        bookingReviewService.closeManagerTodo(reqVO.getReviewId(), getLoginUserId(), reqVO.getRemark());
        return success(true);
    }

    @GetMapping("/dashboard-summary")
    @Operation(summary = "获得预约服务评价看板汇总")
    @PreAuthorize("@ss.hasPermission('booking:review:query')")
    public CommonResult<BookingReviewDashboardRespVO> dashboardSummary() {
        return success(bookingReviewService.getDashboardSummary());
    }
}
