package com.zbkj.front.controller;

import com.zbkj.common.request.ServiceBookingCreateRequest;
import com.zbkj.common.request.ServiceBookingPayRequest;
import com.zbkj.common.response.OrderPayResultResponse;
import com.zbkj.common.response.ServiceBookingOrderResponse;
import com.zbkj.common.response.ServiceBookingSlotResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.service.service.ServiceBookingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 服务预约
 */
@RestController
@RequestMapping("api/front/booking")
@Api(tags = "服务预约")
public class ServiceBookingController {

    @Autowired
    private ServiceBookingService serviceBookingService;

    @ApiOperation(value = "查询排班可用时间槽")
    @GetMapping("/slots/{scheduleId}")
    public CommonResult<List<ServiceBookingSlotResponse>> slots(@PathVariable Integer scheduleId) {
        return CommonResult.success(serviceBookingService.listSlots(scheduleId));
    }

    @ApiOperation(value = "创建服务预约订单")
    @PostMapping("/create")
    public CommonResult<ServiceBookingOrderResponse> create(@Validated @RequestBody ServiceBookingCreateRequest request) {
        return CommonResult.success(serviceBookingService.create(request));
    }

    @ApiOperation(value = "预约订单详情")
    @GetMapping("/detail/{orderNo}")
    public CommonResult<ServiceBookingOrderResponse> detail(@PathVariable String orderNo) {
        return CommonResult.success(serviceBookingService.detail(orderNo));
    }

    @ApiOperation(value = "预约订单支付")
    @PostMapping("/pay")
    public CommonResult<OrderPayResultResponse> pay(@Validated @RequestBody ServiceBookingPayRequest request,
                                                    HttpServletRequest httpServletRequest) {
        String ip = CrmebUtil.getClientIp(httpServletRequest);
        return CommonResult.success(serviceBookingService.pay(request, ip));
    }

    @ApiOperation(value = "取消预约订单")
    @PostMapping("/cancel/{orderNo}")
    public CommonResult<Boolean> cancel(@PathVariable String orderNo) {
        return CommonResult.success(serviceBookingService.cancel(orderNo));
    }
}
