package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 技师佣金记录 Response VO")
@Data
public class TechnicianCommissionRespVO {

    @Schema(description = "佣金记录ID")
    private Long id;

    @Schema(description = "技师ID")
    private Long technicianId;

    @Schema(description = "预约订单ID")
    private Long orderId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "佣金类型 1=基础 2=点钟 3=加钟 4=卡项销售 5=商品 6=好评")
    private Integer commissionType;

    @Schema(description = "订单金额（分）")
    private Integer baseAmount;

    @Schema(description = "佣金比例")
    private BigDecimal commissionRate;

    @Schema(description = "佣金金额（分）")
    private Integer commissionAmount;

    @Schema(description = "状态 0=待结算 1=已结算 2=已取消")
    private Integer status;

    @Schema(description = "结算单ID")
    private Long settlementId;

    @Schema(description = "结算时间")
    private LocalDateTime settlementTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
