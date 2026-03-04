package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - SLA 工单规则 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketSlaRuleRespVO extends TicketSlaRuleBaseVO {

    @Schema(description = "规则ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "创建人", example = "1")
    private String creator;

    @Schema(description = "更新人", example = "1")
    private String updater;

    @Schema(description = "最近动作", example = "ENABLE")
    private String lastAction;

    @Schema(description = "最近动作时间")
    private LocalDateTime lastActionAt;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
