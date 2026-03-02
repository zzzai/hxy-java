package com.hxy.module.booking.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import com.hxy.module.booking.controller.app.vo.AppBookingAddonCreateReqVO;
import com.hxy.module.booking.enums.AddonTypeEnum;
import com.hxy.module.booking.service.BookingAddonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户端 - 预约加钟/升级")
@RestController
@RequestMapping("/app-api/booking/addon")
@Validated
@RequiredArgsConstructor
public class AppBookingAddonController {

    private final BookingAddonService bookingAddonService;

    @PostMapping("/create")
    @Operation(summary = "创建加钟/升级/加项目订单")
    public CommonResult<Long> createAddonOrder(@Valid @RequestBody AppBookingAddonCreateReqVO reqVO) {
        Long userId = getLoginUserId();
        Long orderId;
        if (AddonTypeEnum.EXTEND.getType().equals(reqVO.getAddonType())) {
            orderId = bookingAddonService.createExtendOrder(
                    reqVO.getParentOrderId(), userId, reqVO.getSpuId(), reqVO.getSkuId());
        } else if (AddonTypeEnum.UPGRADE.getType().equals(reqVO.getAddonType())) {
            orderId = bookingAddonService.createUpgradeOrder(
                    reqVO.getParentOrderId(), userId, reqVO.getSkuId());
        } else {
            orderId = bookingAddonService.createAddItemOrder(
                    reqVO.getParentOrderId(), userId, reqVO.getSpuId(), reqVO.getSkuId());
        }
        return success(orderId);
    }

}
