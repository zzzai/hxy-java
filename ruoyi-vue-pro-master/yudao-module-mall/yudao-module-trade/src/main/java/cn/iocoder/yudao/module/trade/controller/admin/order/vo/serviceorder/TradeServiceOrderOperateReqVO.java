package cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 服务履约单操作 Request VO")
@Data
public class TradeServiceOrderOperateReqVO {

    @Schema(description = "服务履约单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "服务履约单编号不能为空")
    private Long id;

    @Schema(description = "备注", example = "到店后开始服务")
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

}
