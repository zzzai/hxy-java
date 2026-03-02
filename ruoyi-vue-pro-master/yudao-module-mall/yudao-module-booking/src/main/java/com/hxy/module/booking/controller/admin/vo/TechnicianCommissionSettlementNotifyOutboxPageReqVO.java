package com.hxy.module.booking.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 技师佣金结算通知出站分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TechnicianCommissionSettlementNotifyOutboxPageReqVO extends PageParam {

    @Schema(description = "结算单 ID", example = "1001")
    private Long settlementId;

    @Schema(description = "通知状态（0 待发送 1 已发送 2 失败）", example = "0")
    private Integer status;

    @Schema(description = "通知类型", example = "P1_WARN")
    private String notifyType;

    @Schema(description = "通知渠道", example = "IN_APP")
    private String channel;

    @Schema(description = "最近审计动作编码", example = "DISPATCH_FAILED")
    private String lastActionCode;

    @Schema(description = "最近审计业务号", example = "OUTBOX#1001")
    private String lastActionBizNo;

}
