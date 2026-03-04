package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleTicketTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - SLA 工单规则命中预览 Request VO")
@Data
public class TicketSlaRulePreviewReqVO {

    @Schema(description = "工单类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "工单类型不能为空")
    @InEnum(value = TicketSlaRuleTicketTypeEnum.class, message = "工单类型必须是 {value}")
    private Integer ticketType;

    @Schema(description = "规则编码", example = "BLACKLIST_USER")
    private String ruleCode;

    @Schema(description = "严重级别", example = "P1")
    private String severity;

    @Schema(description = "门店ID", example = "1001")
    private Long storeId;

}
