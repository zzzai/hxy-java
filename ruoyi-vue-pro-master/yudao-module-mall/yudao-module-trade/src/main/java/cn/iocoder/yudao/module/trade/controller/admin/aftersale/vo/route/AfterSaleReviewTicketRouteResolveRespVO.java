package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工单 SLA 路由命中预览 Response VO")
@Data
public class AfterSaleReviewTicketRouteResolveRespVO {

    @Schema(description = "命中作用域", example = "TYPE_DEFAULT")
    private String matchedScope;

    @Schema(description = "命中规则编号", example = "12")
    private Long routeId;

    @Schema(description = "规则编码", example = "BLACKLIST_USER")
    private String ruleCode;

    @Schema(description = "工单类型", example = "10")
    private Integer ticketType;

    @Schema(description = "严重级别", example = "P1")
    private String severity;

    @Schema(description = "升级责任方", example = "HQ_AFTER_SALE")
    private String escalateTo;

    @Schema(description = "SLA（分钟）", example = "120")
    private Integer slaMinutes;

    @Schema(description = "规则排序", example = "0")
    private Integer sort;

    @Schema(description = "命中决策链", example = "RULE>TYPE_SEVERITY>TYPE_DEFAULT>GLOBAL_DEFAULT")
    private String decisionOrder;

}
