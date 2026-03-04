package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleScopeTypeEnum;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleTicketTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * SLA 工单规则 Base VO
 */
@Data
public class TicketSlaRuleBaseVO {

    @Schema(description = "工单类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "工单类型不能为空")
    @InEnum(value = TicketSlaRuleTicketTypeEnum.class, message = "工单类型必须是 {value}")
    private Integer ticketType;

    @Schema(description = "规则编码（RULE 层级）", example = "BLACKLIST_USER")
    @Size(max = 64, message = "规则编码长度不能超过64")
    private String ruleCode;

    @Schema(description = "严重级别（TYPE_SEVERITY 层级）", example = "P1")
    @Size(max = 16, message = "严重级别长度不能超过16")
    private String severity;

    @Schema(description = "作用域类型：1全局 2门店", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "作用域类型不能为空")
    @InEnum(value = TicketSlaRuleScopeTypeEnum.class, message = "作用域类型必须是 {value}")
    private Integer scopeType;

    @Schema(description = "作用域门店ID（全局可不传）", example = "1001")
    private Long scopeStoreId;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    @Schema(description = "优先级（同层级取最大）", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "优先级不能为空")
    @Min(value = 0, message = "优先级不能小于0")
    private Integer priority;

    @Schema(description = "升级对象", example = "HQ_AFTER_SALE")
    @Size(max = 64, message = "升级对象长度不能超过64")
    private String escalateTo;

    @Schema(description = "SLA分钟", example = "120")
    @Min(value = 1, message = "SLA分钟必须大于0")
    private Integer slaMinutes;

    @Schema(description = "预警提前分钟", example = "30")
    @Min(value = 1, message = "预警提前分钟必须大于0")
    private Integer warnLeadMinutes;

    @Schema(description = "升级延迟分钟", example = "30")
    @Min(value = 1, message = "升级延迟分钟必须大于0")
    private Integer escalateDelayMinutes;

    @Schema(description = "备注", example = "用于高风险售后工单")
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

}
