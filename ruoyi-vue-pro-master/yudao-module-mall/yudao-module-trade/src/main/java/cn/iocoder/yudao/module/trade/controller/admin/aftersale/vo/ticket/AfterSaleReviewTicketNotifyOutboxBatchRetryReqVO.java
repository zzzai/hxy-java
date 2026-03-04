package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - 售后人工复核工单通知出站批量重试 Request VO")
@Data
public class AfterSaleReviewTicketNotifyOutboxBatchRetryReqVO {

    @Schema(description = "通知出站记录 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1001,1002]")
    @NotEmpty(message = "通知出站记录 ID 列表不能为空")
    private List<Long> ids;

    @Schema(description = "重试原因", example = "运营人工触发重试")
    @Size(max = 255, message = "重试原因长度不能超过 255")
    private String reason;
}
