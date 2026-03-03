package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketRouteScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 售后复核工单路由规则分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AfterSaleReviewTicketRoutePageReqVO extends PageParam {

    @Schema(description = "作用域", example = "RULE")
    @InEnum(value = AfterSaleReviewTicketRouteScopeEnum.class, message = "作用域必须是 {value}")
    private String scope;

    @Schema(description = "规则编码", example = "BLACKLIST_USER")
    private String ruleCode;

    @Schema(description = "工单类型", example = "10")
    private Integer ticketType;

    @Schema(description = "严重级别", example = "P1")
    private String severity;

    @Schema(description = "启用状态", example = "true")
    private Boolean enabled;

}
