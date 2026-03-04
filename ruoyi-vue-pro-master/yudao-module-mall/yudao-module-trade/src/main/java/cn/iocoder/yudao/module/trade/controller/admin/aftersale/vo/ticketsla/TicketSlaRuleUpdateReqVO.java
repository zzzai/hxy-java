package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - SLA 工单规则更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketSlaRuleUpdateReqVO extends TicketSlaRuleBaseVO {

    @Schema(description = "规则ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "规则ID不能为空")
    private Long id;

}
