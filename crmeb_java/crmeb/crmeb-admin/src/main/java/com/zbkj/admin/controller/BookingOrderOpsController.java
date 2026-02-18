package com.zbkj.admin.controller;

import com.zbkj.common.request.ServiceBookingVerifyRequest;
import com.zbkj.common.response.ServiceBookingCardCheckResponse;
import com.zbkj.common.response.ServiceBookingVerifyRecordResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.ServiceBookingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

/**
 * 预约订单运维接口
 */
@RestController
@RequestMapping("api/admin/booking/order")
@Api(tags = "预约订单运维")
@PreAuthorize("hasAuthority('admin:order:list')")
public class BookingOrderOpsController {

    @Autowired
    private ServiceBookingService serviceBookingService;

    @ApiOperation(value = "手动确认预约订单支付成功")
    @PreAuthorize("hasAuthority('admin:order:write:update')")
    @PostMapping("/pay/success/{orderNo}")
    public CommonResult<Boolean> paySuccess(@PathVariable String orderNo) {
        return CommonResult.success(serviceBookingService.paySuccess(orderNo));
    }

    @ApiOperation(value = "释放超时锁单")
    @PreAuthorize("hasAuthority('admin:order:write:update')")
    @PostMapping("/release/expired")
    public CommonResult<Integer> releaseExpired(@RequestParam(value = "limit", required = false) Integer limit) {
        return CommonResult.success(serviceBookingService.releaseExpiredLocks(limit));
    }

    @ApiOperation(value = "核销预约订单")
    @PreAuthorize("hasAuthority('admin:order:write:update')")
    @PostMapping("/verify")
    public CommonResult<Boolean> verify(@Valid @RequestBody ServiceBookingVerifyRequest request) {
        return CommonResult.success(serviceBookingService.verify(request));
    }

    @ApiOperation(value = "检查预约订单会员卡可用性")
    @PreAuthorize("hasAuthority('admin:order:write:confirm')")
    @GetMapping("/verify/check-card/{orderNo}")
    public CommonResult<ServiceBookingCardCheckResponse> checkCard(@PathVariable String orderNo,
                                                                   @RequestParam(value = "usageTimes", required = false) Integer usageTimes,
                                                                   @RequestParam(value = "usageAmount", required = false) BigDecimal usageAmount) {
        return CommonResult.success(serviceBookingService.checkMemberCard(orderNo, usageTimes, usageAmount));
    }

    @ApiOperation(value = "查询预约订单核销流水")
    @PreAuthorize("hasAuthority('admin:order:write:confirm')")
    @GetMapping("/verify/records/{orderNo}")
    public CommonResult<List<ServiceBookingVerifyRecordResponse>> verifyRecords(@PathVariable String orderNo) {
        return CommonResult.success(serviceBookingService.listVerifyRecords(orderNo));
    }
}
