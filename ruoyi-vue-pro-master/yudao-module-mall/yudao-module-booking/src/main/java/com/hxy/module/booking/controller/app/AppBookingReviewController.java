package com.hxy.module.booking.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewCreateReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewEligibilityRespVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewPageReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewRespVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewSummaryRespVO;
import com.hxy.module.booking.convert.BookingReviewConvert;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.service.BookingReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户端 - 预约服务评价")
@RestController
@RequestMapping("/booking/review")
@Validated
@RequiredArgsConstructor
public class AppBookingReviewController {

    private final BookingReviewService bookingReviewService;

    @GetMapping("/eligibility")
    @Operation(summary = "获取预约订单评价资格")
    public CommonResult<AppBookingReviewEligibilityRespVO> getEligibility(
            @Parameter(name = "bookingOrderId", required = true) @RequestParam("bookingOrderId") Long bookingOrderId) {
        return success(bookingReviewService.getEligibility(getLoginUserId(), bookingOrderId));
    }

    @PostMapping("/create")
    @Operation(summary = "创建预约服务评价")
    public CommonResult<Long> createReview(@Valid @RequestBody AppBookingReviewCreateReqVO reqVO) {
        return success(bookingReviewService.createReview(getLoginUserId(), reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "分页获取我的预约服务评价")
    public CommonResult<PageResult<AppBookingReviewRespVO>> getReviewPage(@Valid AppBookingReviewPageReqVO reqVO) {
        PageResult<BookingReviewDO> pageResult = bookingReviewService.getReviewPage(getLoginUserId(), reqVO);
        return success(new PageResult<>(BookingReviewConvert.INSTANCE.convertList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "获取预约服务评价详情")
    public CommonResult<AppBookingReviewRespVO> getReview(
            @Parameter(name = "id", required = true) @RequestParam("id") Long id) {
        return success(BookingReviewConvert.INSTANCE.convert(bookingReviewService.getReview(getLoginUserId(), id)));
    }

    @GetMapping("/summary")
    @Operation(summary = "获取我的预约服务评价汇总")
    public CommonResult<AppBookingReviewSummaryRespVO> getSummary() {
        return success(bookingReviewService.getSummary(getLoginUserId()));
    }
}
