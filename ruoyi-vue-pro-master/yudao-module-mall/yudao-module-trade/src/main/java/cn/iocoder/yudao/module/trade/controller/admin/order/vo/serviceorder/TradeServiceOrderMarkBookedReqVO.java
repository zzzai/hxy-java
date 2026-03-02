package cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 服务履约单标记已预约 Request VO")
@Data
public class TradeServiceOrderMarkBookedReqVO {

    @Schema(description = "服务履约单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "服务履约单编号不能为空")
    private Long id;

    @Schema(description = "预约单号", example = "BOOK202602240001")
    @Size(max = 64, message = "预约单号长度不能超过 64")
    private String bookingNo;

    @Schema(description = "备注", example = "门店已确认预约时间")
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

}
