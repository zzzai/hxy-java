package com.hxy.module.booking.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "用户端 - 创建预约订单 Request VO")
@Data
public class AppBookingOrderCreateReqVO {

    @Schema(description = "时间槽编号（点钟模式必填）", example = "1")
    private Long timeSlotId;

    @Schema(description = "服务商品SPU编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "服务商品编号不能为空")
    private Long spuId;

    @Schema(description = "服务商品SKU编号", example = "1")
    private Long skuId;

    @Schema(description = "用户备注", example = "请轻一点")
    private String userRemark;

    @Schema(description = "派单模式：1=点钟 2=排钟", example = "1")
    private Integer dispatchMode;

    @Schema(description = "门店编号（排钟模式必填）", example = "1")
    private Long storeId;

    @Schema(description = "预约日期（排钟模式必填）", example = "2026-03-01")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    @Schema(description = "开始时间（排钟模式必填）", example = "09:00")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

}
