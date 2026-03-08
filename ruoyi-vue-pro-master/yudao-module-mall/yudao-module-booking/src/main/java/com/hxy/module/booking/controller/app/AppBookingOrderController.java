package com.hxy.module.booking.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayRefundNotifyReqDTO;
import com.hxy.module.booking.controller.app.vo.*;
import com.hxy.module.booking.service.support.FinanceLogFieldValidator;
import com.hxy.module.booking.convert.BookingOrderConvert;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.service.BookingOrderService;
import com.hxy.module.booking.service.BookingRefundNotifyLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户端 - 预约订单")
@RestController
@RequestMapping("/booking/order")
@Validated
@RequiredArgsConstructor
@Slf4j
public class AppBookingOrderController {

    private static final Pattern REFUND_BIZ_NO_PATTERN = Pattern.compile("^(\\d+)(?:-refund)?$");

    private final BookingOrderService bookingOrderService;
    private final BookingRefundNotifyLogService refundNotifyLogService;

    @PostMapping("/create")
    @Operation(summary = "创建预约订单")
    public CommonResult<Long> createOrder(@Valid @RequestBody AppBookingOrderCreateReqVO reqVO) {
        Long orderId = bookingOrderService.createOrder(
                getLoginUserId(),
                reqVO.getTimeSlotId(),
                reqVO.getSpuId(),
                reqVO.getSkuId(),
                reqVO.getUserRemark(),
                reqVO.getDispatchMode(),
                reqVO.getStoreId(),
                reqVO.getBookingDate(),
                reqVO.getStartTime()
        );
        return success(orderId);
    }

    @GetMapping("/get")
    @Operation(summary = "获取预约订单")
    @Parameter(name = "id", description = "订单编号", required = true)
    public CommonResult<AppBookingOrderRespVO> getOrder(@RequestParam("id") Long id) {
        BookingOrderDO order = bookingOrderService.getOrderByUser(id, getLoginUserId());
        return success(BookingOrderConvert.INSTANCE.convert(order));
    }

    @GetMapping("/get-by-order-no")
    @Operation(summary = "根据订单号获取预约订单")
    @Parameter(name = "orderNo", description = "订单号", required = true)
    public CommonResult<AppBookingOrderRespVO> getOrderByOrderNo(@RequestParam("orderNo") String orderNo) {
        BookingOrderDO order = bookingOrderService.getOrderByOrderNoAndUser(orderNo, getLoginUserId());
        return success(BookingOrderConvert.INSTANCE.convert(order));
    }

    @GetMapping("/list")
    @Operation(summary = "获取我的预约订单列表")
    public CommonResult<List<AppBookingOrderRespVO>> getOrderList() {
        List<BookingOrderDO> list = bookingOrderService.getOrderListByUserId(getLoginUserId());
        return success(BookingOrderConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-status")
    @Operation(summary = "根据状态获取我的预约订单列表")
    @Parameter(name = "status", description = "订单状态", required = true)
    public CommonResult<List<AppBookingOrderRespVO>> getOrderListByStatus(@RequestParam("status") Integer status) {
        List<BookingOrderDO> list = bookingOrderService.getOrderListByUserIdAndStatus(getLoginUserId(), status);
        return success(BookingOrderConvert.INSTANCE.convertList(list));
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消预约订单")
    public CommonResult<Boolean> cancelOrder(@RequestParam("id") Long id,
                                             @RequestParam(value = "reason", required = false) String reason) {
        bookingOrderService.cancelOrder(id, getLoginUserId(), reason);
        return success(true);
    }

    @PostMapping("/update-refunded")
    @Operation(summary = "更新预约订单为已退款")
    @PermitAll
    public CommonResult<Boolean> updateOrderRefunded(@Valid @RequestBody PayRefundNotifyReqDTO notifyReqDTO) {
        Long orderId = null;
        FinanceLogFieldValidator.FinanceLogFields beginFields = validateFinanceLogFields(
                null, notifyReqDTO == null ? null : notifyReqDTO.getPayRefundId(),
                notifyReqDTO == null ? null : notifyReqDTO.getMerchantRefundId(), "CALLBACK_RECEIVED");
        log.info("[finance-audit][scene=booking_refund_callback_received][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                beginFields.getRunId(), beginFields.getOrderId(), beginFields.getPayRefundId(),
                beginFields.getSourceBizNo(), beginFields.getErrorCode());
        try {
            orderId = parseOrderId(notifyReqDTO.getMerchantRefundId());
            bookingOrderService.updateOrderRefunded(orderId, notifyReqDTO.getPayRefundId());
            refundNotifyLogService.recordNotifySuccess(orderId, notifyReqDTO);
            FinanceLogFieldValidator.FinanceLogFields successFields = validateFinanceLogFields(
                    orderId, notifyReqDTO.getPayRefundId(), notifyReqDTO.getMerchantRefundId(), "CALLBACK_SUCCESS");
            log.info("[finance-audit][scene=booking_refund_callback_success][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                    successFields.getRunId(), successFields.getOrderId(), successFields.getPayRefundId(),
                    successFields.getSourceBizNo(), successFields.getErrorCode());
            return success(true);
        } catch (Exception ex) {
            refundNotifyLogService.recordNotifyFailure(orderId, notifyReqDTO, ex);
            FinanceLogFieldValidator.FinanceLogFields failFields = validateFinanceLogFields(
                    orderId, notifyReqDTO == null ? null : notifyReqDTO.getPayRefundId(),
                    notifyReqDTO == null ? null : notifyReqDTO.getMerchantRefundId(), ex.getClass().getSimpleName());
            log.warn("[finance-audit][scene=booking_refund_callback_failed][runId={}][orderId={}][payRefundId={}][sourceBizNo={}][errorCode={}]",
                    failFields.getRunId(), failFields.getOrderId(), failFields.getPayRefundId(),
                    failFields.getSourceBizNo(), failFields.getErrorCode(), ex);
            throw ex;
        }
    }

    private Long parseOrderId(String merchantRefundId) {
        Matcher matcher = REFUND_BIZ_NO_PATTERN.matcher(merchantRefundId == null ? "" : merchantRefundId.trim());
        if (!matcher.matches()) {
            throw exception(BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID);
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            throw exception(BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID);
        }
    }

    private FinanceLogFieldValidator.FinanceLogFields validateFinanceLogFields(Long orderId, Long payRefundId,
                                                                                String sourceBizNo, String errorCode) {
        FinanceLogFieldValidator.FinanceLogFields fields = FinanceLogFieldValidator.validate(
                "NO_RUN", orderId, payRefundId, sourceBizNo, errorCode);
        if (!fields.isComplete()) {
            log.warn("[finance-log-validate][scene=booking_refund_callback][missingFields={}]",
                    fields.getMissingFields());
        }
        return fields;
    }

}
