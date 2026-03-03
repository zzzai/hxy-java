package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 售后复核工单路由规则 Response VO")
@Data
public class AfterSaleReviewTicketRouteRespVO {

    @Schema(description = "规则编号", example = "1")
    private Long id;

    @Schema(description = "作用域", example = "RULE")
    private String scope;

    @Schema(description = "规则编码", example = "BLACKLIST_USER")
    private String ruleCode;

    @Schema(description = "工单类型", example = "10")
    private Integer ticketType;

    @Schema(description = "严重级别", example = "P1")
    private String severity;

    @Schema(description = "升级对象", example = "HQ_RISK_FINANCE")
    private String escalateTo;

    @Schema(description = "SLA 分钟", example = "30")
    private Integer slaMinutes;

    @Schema(description = "启用状态", example = "true")
    private Boolean enabled;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "备注", example = "风险单默认路由")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
