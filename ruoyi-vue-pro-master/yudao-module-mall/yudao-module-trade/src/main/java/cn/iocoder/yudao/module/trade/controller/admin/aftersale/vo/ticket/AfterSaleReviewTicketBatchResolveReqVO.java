package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - 售后人工复核工单批量收口 Request VO")
@Data
public class AfterSaleReviewTicketBatchResolveReqVO {

    @Schema(description = "工单 ID 列表（会去重）", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1,2,3]")
    @NotEmpty(message = "工单 ID 列表不能为空")
    private List<Long> ids;

    @Schema(description = "收口备注", example = "批量人工复核通过")
    @Size(max = 255, message = "收口备注长度不能超过 255")
    private String resolveRemark;

    @Schema(description = "收口动作编码", example = "MANUAL_RESOLVE")
    @Size(max = 64, message = "收口动作编码长度不能超过 64")
    private String resolveActionCode;

    @Schema(description = "收口来源业务号（为空则回退工单 ID）", example = "OPS-BATCH-20260303")
    @Size(max = 64, message = "收口来源业务号长度不能超过 64")
    private String resolveBizNo;

}
