package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingRespVO;
import com.hxy.module.booking.service.BookingReviewManagerAccountRoutingQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 预约评价店长账号路由核查")
@RestController
@RequestMapping("/booking/review/manager-routing")
@Validated
public class BookingReviewManagerAccountRoutingController {

    @Resource
    private BookingReviewManagerAccountRoutingQueryService bookingReviewManagerAccountRoutingQueryService;

    @GetMapping("/get")
    @Operation(summary = "获得门店店长账号路由核查结果")
    @Parameter(name = "storeId", description = "门店ID", required = true, example = "3001")
    @PreAuthorize("@ss.hasPermission('booking:review:query')")
    public CommonResult<BookingReviewManagerAccountRoutingRespVO> get(@RequestParam("storeId") Long storeId) {
        return success(bookingReviewManagerAccountRoutingQueryService.getRouting(storeId));
    }

    @GetMapping("/page")
    @Operation(summary = "分页获得门店店长账号路由核查结果")
    @PreAuthorize("@ss.hasPermission('booking:review:query')")
    public CommonResult<PageResult<BookingReviewManagerAccountRoutingRespVO>> page(
            @Valid BookingReviewManagerAccountRoutingPageReqVO reqVO) {
        return success(bookingReviewManagerAccountRoutingQueryService.getRoutingPage(reqVO));
    }
}
