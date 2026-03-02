package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 售后人工复核工单收口 Request VO")
@Data
public class AfterSaleReviewTicketResolveReqVO {

    @Schema(description = "工单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "工单 ID 不能为空")
    private Long id;

    @Schema(description = "收口备注", example = "已人工复核通过")
    @Size(max = 255, message = "收口备注长度不能超过 255")
    private String resolveRemark;

    @Schema(description = "收口动作编码", example = "MANUAL_RESOLVE")
    @Size(max = 64, message = "收口动作编码长度不能超过 64")
    private String resolveActionCode;

    @Schema(description = "收口来源业务号", example = "OPS-202603020001")
    @Size(max = 64, message = "收口来源业务号长度不能超过 64")
    private String resolveBizNo;

}
