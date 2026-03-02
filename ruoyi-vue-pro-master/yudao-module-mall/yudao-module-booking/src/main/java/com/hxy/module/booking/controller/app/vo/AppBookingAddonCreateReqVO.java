package com.hxy.module.booking.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "用户端 - 加钟/升级/加项目 Request VO")
@Data
public class AppBookingAddonCreateReqVO {

    @Schema(description = "原订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "原订单编号不能为空")
    private Long parentOrderId;

    @Schema(description = "加钟类型：1=加钟 2=升级 3=加项目", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "加钟类型不能为空")
    private Integer addonType;

    @Schema(description = "服务商品SPU编号", example = "1")
    private Long spuId;

    @Schema(description = "服务商品SKU编号", example = "1")
    private Long skuId;

}
