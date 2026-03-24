package cn.iocoder.yudao.module.promotion.controller.app.giftcard;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.promotion.service.giftcard.GiftCardService;
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

import javax.annotation.security.PermitAll;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 礼品卡")
@RestController
@RequestMapping("/promotion/gift-card")
@Validated
@RequiredArgsConstructor
public class AppGiftCardController {

    private final GiftCardService giftCardService;

    @GetMapping("/template/page")
    @PermitAll
    @Operation(summary = "分页获取礼品卡模板")
    public CommonResult<PageResult<AppGiftCardTemplateRespVO>> getTemplatePage(@Valid AppGiftCardTemplatePageReqVO reqVO) {
        return success(giftCardService.getTemplatePage(reqVO));
    }

    @PostMapping("/order/create")
    @Operation(summary = "创建礼品卡订单")
    public CommonResult<AppGiftCardOrderCreateRespVO> createOrder(@Valid @RequestBody AppGiftCardOrderCreateReqVO reqVO) {
        return success(giftCardService.createOrder(getLoginUserId(), reqVO));
    }

    @GetMapping("/order/get")
    @Operation(summary = "获取礼品卡订单详情")
    public CommonResult<AppGiftCardOrderRespVO> getOrder(
            @Parameter(name = "orderId", required = true) @RequestParam("orderId") Long orderId) {
        return success(giftCardService.getOrder(getLoginUserId(), orderId));
    }

    @PostMapping("/redeem")
    @Operation(summary = "核销礼品卡")
    public CommonResult<AppGiftCardRedeemRespVO> redeem(@Valid @RequestBody AppGiftCardRedeemReqVO reqVO) {
        return success(giftCardService.redeem(getLoginUserId(), reqVO));
    }

    @PostMapping("/refund/apply")
    @Operation(summary = "申请礼品卡退款")
    public CommonResult<AppGiftCardRefundApplyRespVO> applyRefund(
            @Valid @RequestBody AppGiftCardRefundApplyReqVO reqVO) {
        return success(giftCardService.applyRefund(getLoginUserId(), reqVO));
    }
}
