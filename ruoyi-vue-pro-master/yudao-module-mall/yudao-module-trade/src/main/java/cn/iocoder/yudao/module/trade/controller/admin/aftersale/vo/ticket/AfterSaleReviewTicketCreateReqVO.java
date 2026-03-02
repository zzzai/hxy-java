package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 统一工单创建 Request VO")
@Data
public class AfterSaleReviewTicketCreateReqVO {

    @Schema(description = "工单类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @NotNull(message = "工单类型不能为空")
    @InEnum(value = AfterSaleReviewTicketTypeEnum.class, message = "工单类型必须是 {value}")
    private Integer ticketType;

    @Schema(description = "售后单ID（售后复核工单可传）", example = "1001")
    private Long afterSaleId;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "订单项ID", example = "3001")
    private Long orderItemId;

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "4001")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "来源业务单号", example = "BK202603010001")
    private String sourceBizNo;

    @Schema(description = "规则编码", example = "SERVICE_DELAY")
    private String ruleCode;

    @Schema(description = "命中原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "命中原因不能为空")
    private String decisionReason;

    @Schema(description = "严重级别，默认P1", example = "P1")
    private String severity;

    @Schema(description = "升级对象", example = "HQ_AFTER_SALE")
    private String escalateTo;

    @Schema(description = "SLA分钟，默认120", example = "120")
    private Integer slaMinutes;

    @Schema(description = "备注")
    private String remark;
}
