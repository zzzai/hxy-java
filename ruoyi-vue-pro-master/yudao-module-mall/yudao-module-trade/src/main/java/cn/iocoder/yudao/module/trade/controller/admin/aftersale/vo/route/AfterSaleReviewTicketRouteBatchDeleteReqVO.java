package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - 工单 SLA 路由规则批量删除 Request VO")
@Data
public class AfterSaleReviewTicketRouteBatchDeleteReqVO {

    @Schema(description = "规则编号列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1,2,3]")
    @NotEmpty(message = "规则编号列表不能为空")
    private List<Long> ids;

}
