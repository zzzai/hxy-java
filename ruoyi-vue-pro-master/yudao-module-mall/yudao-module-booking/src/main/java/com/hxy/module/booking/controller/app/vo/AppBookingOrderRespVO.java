package com.hxy.module.booking.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "用户端 - 预约订单 Response VO")
@Data
public class AppBookingOrderRespVO {

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BK123456789")
    private String orderNo;

    @Schema(description = "门店编号", example = "1")
    private Long storeId;

    @Schema(description = "门店名称", example = "荷小悦旗舰店")
    private String storeName;

    @Schema(description = "技师编号", example = "1")
    private Long technicianId;

    @Schema(description = "技师姓名", example = "张三")
    private String technicianName;

    @Schema(description = "技师头像", example = "https://xxx.com/avatar.jpg")
    private String technicianAvatar;

    @Schema(description = "服务名称", example = "全身按摩")
    private String serviceName;

    @Schema(description = "服务图片", example = "https://xxx.com/service.jpg")
    private String servicePic;

    @Schema(description = "预约日期", example = "2026-02-24")
    private LocalDate bookingDate;

    @Schema(description = "预约开始时间", example = "09:00")
    private LocalTime bookingStartTime;

    @Schema(description = "预约结束时间", example = "10:00")
    private LocalTime bookingEndTime;

    @Schema(description = "服务时长（分钟）", example = "60")
    private Integer duration;

    @Schema(description = "原价（分）", example = "10000")
    private Integer originalPrice;

    @Schema(description = "优惠金额（分）", example = "2000")
    private Integer discountPrice;

    @Schema(description = "实付金额（分）", example = "8000")
    private Integer payPrice;

    @Schema(description = "是否闲时优惠", example = "true")
    private Boolean isOffpeak;

    @Schema(description = "订单状态", example = "0")
    private Integer status;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "服务开始时间")
    private LocalDateTime serviceStartTime;

    @Schema(description = "服务结束时间")
    private LocalDateTime serviceEndTime;

    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;

    @Schema(description = "取消原因", example = "临时有事")
    private String cancelReason;

    @Schema(description = "用户备注", example = "请轻一点")
    private String userRemark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
