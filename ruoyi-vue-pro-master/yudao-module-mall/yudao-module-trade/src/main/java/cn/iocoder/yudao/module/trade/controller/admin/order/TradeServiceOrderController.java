package cn.iocoder.yudao.module.trade.controller.admin.order;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder.TradeServiceOrderMarkBookedReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder.TradeServiceOrderOperateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder.TradeServiceOrderPageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder.TradeServiceOrderRespVO;
import cn.iocoder.yudao.module.trade.convert.order.TradeServiceOrderConvert;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import cn.iocoder.yudao.module.trade.service.order.TradeServiceOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 服务履约单")
@RestController
@RequestMapping("/trade/service-order")
@Validated
public class TradeServiceOrderController {

    @Resource
    private TradeServiceOrderService tradeServiceOrderService;

    @GetMapping("/page")
    @Operation(summary = "获得服务履约单分页")
    @PreAuthorize("@ss.hasPermission('trade:order:query')")
    public CommonResult<PageResult<TradeServiceOrderRespVO>> getServiceOrderPage(
            @Valid TradeServiceOrderPageReqVO pageReqVO) {
        PageResult<TradeServiceOrderDO> pageResult = tradeServiceOrderService.getServiceOrderPage(pageReqVO);
        return success(TradeServiceOrderConvert.INSTANCE.convertPageWithMeta(pageResult));
    }

    @GetMapping("/get")
    @Operation(summary = "获得服务履约单详情")
    @Parameter(name = "id", description = "服务履约单编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('trade:order:query')")
    public CommonResult<TradeServiceOrderRespVO> getServiceOrder(@RequestParam("id") Long id) {
        return success(TradeServiceOrderConvert.INSTANCE
                .convertWithMeta(tradeServiceOrderService.getServiceOrder(id)));
    }

    @GetMapping("/list-by-order-id")
    @Operation(summary = "按交易订单编号获得服务履约单列表")
    @Parameter(name = "orderId", description = "交易订单编号", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('trade:order:query')")
    public CommonResult<List<TradeServiceOrderRespVO>> getServiceOrderListByOrderId(
            @RequestParam("orderId") Long orderId) {
        return success(TradeServiceOrderConvert.INSTANCE
                .convertListWithMeta(tradeServiceOrderService.getServiceOrderListByOrderId(orderId)));
    }

    @PutMapping("/mark-booked")
    @Operation(summary = "服务履约单标记为已预约")
    @PreAuthorize("@ss.hasPermission('trade:order:update')")
    public CommonResult<Boolean> markBooked(@Valid @RequestBody TradeServiceOrderMarkBookedReqVO reqVO) {
        tradeServiceOrderService.markBooked(reqVO.getId(), reqVO.getBookingNo(), reqVO.getRemark());
        return success(true);
    }

    @PutMapping("/start-serving")
    @Operation(summary = "服务履约单标记为服务中")
    @PreAuthorize("@ss.hasPermission('trade:order:update')")
    public CommonResult<Boolean> startServing(@Valid @RequestBody TradeServiceOrderOperateReqVO reqVO) {
        tradeServiceOrderService.startServing(reqVO.getId(), reqVO.getRemark());
        return success(true);
    }

    @PutMapping("/finish-serving")
    @Operation(summary = "服务履约单标记为已完成")
    @PreAuthorize("@ss.hasPermission('trade:order:update')")
    public CommonResult<Boolean> finishServing(@Valid @RequestBody TradeServiceOrderOperateReqVO reqVO) {
        tradeServiceOrderService.finishServing(reqVO.getId(), reqVO.getRemark());
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消服务履约单")
    @PreAuthorize("@ss.hasPermission('trade:order:update')")
    public CommonResult<Boolean> cancel(@Valid @RequestBody TradeServiceOrderOperateReqVO reqVO) {
        tradeServiceOrderService.cancelServiceOrder(reqVO.getId(), reqVO.getRemark());
        return success(true);
    }

}
