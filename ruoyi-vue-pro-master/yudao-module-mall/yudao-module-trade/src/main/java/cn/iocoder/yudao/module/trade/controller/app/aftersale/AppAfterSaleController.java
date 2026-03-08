package cn.iocoder.yudao.module.trade.controller.app.aftersale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pay.api.refund.PayRefundApi;
import cn.iocoder.yudao.module.pay.api.refund.dto.PayRefundRespDTO;
import cn.iocoder.yudao.module.pay.enums.refund.PayRefundStatusEnum;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSaleCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSaleDeliveryReqVO;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSalePageReqVO;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSaleRefundProgressRespVO;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSaleRespVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleService;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 交易售后")
@RestController
@RequestMapping("/trade/after-sale")
@Validated
@Slf4j
public class AppAfterSaleController {

    @Resource
    private AfterSaleService afterSaleService;
    @Resource
    private PayRefundApi payRefundApi;

    @GetMapping(value = "/page")
    @Operation(summary = "获得售后分页")
    public CommonResult<PageResult<AppAfterSaleRespVO>> getAfterSalePage(AppAfterSalePageReqVO pageReqVO) {
        PageResult<AfterSaleDO> pageResult = afterSaleService.getAfterSalePage(getLoginUserId(), pageReqVO);
        return success(BeanUtils.toBean(pageResult, AppAfterSaleRespVO.class));
    }

    @GetMapping(value = "/get")
    @Operation(summary = "获得售后订单")
    @Parameter(name = "id", description = "售后编号", required = true, example = "1")
    public CommonResult<AppAfterSaleRespVO> getAfterSale(@RequestParam("id") Long id) {
        AfterSaleDO afterSale = afterSaleService.getAfterSale(getLoginUserId(), id);
        return success(BeanUtils.toBean(afterSale, AppAfterSaleRespVO.class));
    }

    @PostMapping(value = "/create")
    @Operation(summary = "申请售后")
    public CommonResult<Long> createAfterSale(@RequestBody AppAfterSaleCreateReqVO createReqVO) {
        return success(afterSaleService.createAfterSale(getLoginUserId(), createReqVO));
    }

    @PutMapping(value = "/delivery")
    @Operation(summary = "退回货物")
    public CommonResult<Boolean> deliveryAfterSale(@RequestBody AppAfterSaleDeliveryReqVO deliveryReqVO) {
        afterSaleService.deliveryAfterSale(getLoginUserId(), deliveryReqVO);
        return success(true);
    }

    @DeleteMapping(value = "/cancel")
    @Operation(summary = "取消售后")
    @Parameter(name = "id", description = "售后编号", required = true, example = "1")
    public CommonResult<Boolean> cancelAfterSale(@RequestParam("id") Long id) {
        afterSaleService.cancelAfterSale(getLoginUserId(), id);
        return success(true);
    }

    @GetMapping(value = "/refund-progress")
    @Operation(summary = "获得退款进度聚合（按订单或售后维度）")
    @Parameters({
            @Parameter(name = "orderId", description = "订单编号", example = "1001"),
            @Parameter(name = "afterSaleId", description = "售后编号", example = "2001")
    })
    public CommonResult<AppAfterSaleRefundProgressRespVO> getRefundProgress(
            @RequestParam(value = "orderId", required = false) Long orderId,
            @RequestParam(value = "afterSaleId", required = false) Long afterSaleId) {
        if (orderId == null && afterSaleId == null) {
            return success(null);
        }
        AfterSaleDO afterSale = afterSaleId != null
                ? afterSaleService.getAfterSale(getLoginUserId(), afterSaleId)
                : afterSaleService.getLatestAfterSaleByOrderId(getLoginUserId(), orderId);
        if (afterSale == null) {
            return success(null);
        }
        PayRefundRespDTO payRefund = null;
        if (afterSale.getPayRefundId() != null) {
            payRefund = payRefundApi.getRefund(afterSale.getPayRefundId());
        }
        AppAfterSaleRefundProgressRespVO respVO = new AppAfterSaleRefundProgressRespVO();
        respVO.setAfterSaleId(afterSale.getId());
        respVO.setAfterSaleNo(afterSale.getNo());
        respVO.setOrderId(afterSale.getOrderId());
        respVO.setOrderNo(afterSale.getOrderNo());
        respVO.setAfterSaleStatus(afterSale.getStatus());
        respVO.setAfterSaleStatusName(resolveAfterSaleStatusName(afterSale.getStatus()));
        respVO.setRefundPrice(afterSale.getRefundPrice());
        respVO.setPayRefundId(afterSale.getPayRefundId());
        respVO.setRefundTime(afterSale.getRefundTime());
        if (payRefund != null) {
            respVO.setPayRefundStatus(payRefund.getStatus());
            respVO.setPayRefundStatusName(resolvePayRefundStatusName(payRefund.getStatus()));
            respVO.setMerchantOrderId(payRefund.getMerchantOrderId());
            respVO.setMerchantRefundId(payRefund.getMerchantRefundId());
            respVO.setChannelErrorCode(payRefund.getChannelErrorCode());
            respVO.setChannelErrorMsg(payRefund.getChannelErrorMsg());
            if (respVO.getRefundTime() == null) {
                respVO.setRefundTime(payRefund.getSuccessTime());
            }
        }
        respVO.setProgressCode(resolveRefundProgressCode(afterSale.getStatus(), respVO.getPayRefundStatus()));
        respVO.setProgressDesc(resolveRefundProgressDesc(respVO.getProgressCode()));
        return success(respVO);
    }

    private String resolveAfterSaleStatusName(Integer status) {
        AfterSaleStatusEnum statusEnum = AfterSaleStatusEnum.valueOf(status);
        return statusEnum == null ? "未知" : statusEnum.getName();
    }

    private String resolvePayRefundStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        for (PayRefundStatusEnum statusEnum : PayRefundStatusEnum.values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum.getName();
            }
        }
        return "未知";
    }

    private String resolveRefundProgressCode(Integer afterSaleStatus, Integer payRefundStatus) {
        if (PayRefundStatusEnum.SUCCESS.getStatus().equals(payRefundStatus)) {
            return "REFUND_SUCCESS";
        }
        if (PayRefundStatusEnum.FAILURE.getStatus().equals(payRefundStatus)) {
            return "REFUND_FAILED";
        }
        if (AfterSaleStatusEnum.COMPLETE.getStatus().equals(afterSaleStatus)) {
            return "REFUND_SUCCESS";
        }
        if (AfterSaleStatusEnum.BUYER_CANCEL.getStatus().equals(afterSaleStatus)
                || AfterSaleStatusEnum.SELLER_DISAGREE.getStatus().equals(afterSaleStatus)
                || AfterSaleStatusEnum.SELLER_REFUSE.getStatus().equals(afterSaleStatus)) {
            return "REFUND_FAILED";
        }
        if (AfterSaleStatusEnum.WAIT_REFUND.getStatus().equals(afterSaleStatus)
                || PayRefundStatusEnum.WAITING.getStatus().equals(payRefundStatus)) {
            return "REFUND_PROCESSING";
        }
        return "REFUND_PENDING";
    }

    private String resolveRefundProgressDesc(String progressCode) {
        if ("REFUND_SUCCESS".equals(progressCode)) {
            return "退款成功";
        }
        if ("REFUND_FAILED".equals(progressCode)) {
            return "退款失败";
        }
        if ("REFUND_PROCESSING".equals(progressCode)) {
            return "退款处理中";
        }
        return "等待审核";
    }

}
