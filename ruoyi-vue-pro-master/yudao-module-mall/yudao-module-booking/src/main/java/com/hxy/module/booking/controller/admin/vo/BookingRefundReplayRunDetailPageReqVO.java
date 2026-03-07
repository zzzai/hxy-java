package com.hxy.module.booking.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - booking退款回调重放批次明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingRefundReplayRunDetailPageReqVO extends PageParam {

    @Schema(description = "批次号", example = "RR202603071200000001")
    private String runId;

    @Schema(description = "结果状态(SUCCESS/SKIP/FAIL)", example = "FAIL")
    private String resultStatus;

    @Schema(description = "结果码", example = "1030004012")
    private String resultCode;

    @Schema(description = "告警标签", example = "FOUR_ACCOUNT_REFRESH_WARN")
    private String warningTag;

    @Schema(description = "工单同步状态(SUCCESS/SKIP/FAIL)", example = "FAIL")
    private String ticketSyncStatus;

    @Schema(description = "退款回调台账ID", example = "100")
    private Long notifyLogId;

    @Schema(description = "预约订单ID", example = "1001")
    private Long orderId;

    @Schema(description = "支付退款单ID", example = "9001")
    private Long payRefundId;
}
