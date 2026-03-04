package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleScopeTypeEnum;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleTicketTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - SLA 工单规则分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketSlaRulePageReqVO extends PageParam {

    @Schema(description = "工单类型", example = "10")
    @InEnum(value = TicketSlaRuleTicketTypeEnum.class, message = "工单类型必须是 {value}")
    private Integer ticketType;

    @Schema(description = "规则编码", example = "BLACKLIST_USER")
    private String ruleCode;

    @Schema(description = "严重级别", example = "P1")
    private String severity;

    @Schema(description = "作用域类型", example = "1")
    @InEnum(value = TicketSlaRuleScopeTypeEnum.class, message = "作用域类型必须是 {value}")
    private Integer scopeType;

    @Schema(description = "作用域门店ID", example = "1001")
    private Long scopeStoreId;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

}
