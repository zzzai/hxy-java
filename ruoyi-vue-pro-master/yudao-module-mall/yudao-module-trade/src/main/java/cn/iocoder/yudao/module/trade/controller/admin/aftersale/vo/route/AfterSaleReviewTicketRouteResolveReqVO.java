package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工单 SLA 路由命中预览 Request VO")
@Data
public class AfterSaleReviewTicketRouteResolveReqVO {

    @Schema(description = "规则编码", example = "BLACKLIST_USER")
    private String ruleCode;

    @Schema(description = "工单类型", example = "10")
    private Integer ticketType;

    @Schema(description = "严重级别", example = "P1")
    private String severity;

}
