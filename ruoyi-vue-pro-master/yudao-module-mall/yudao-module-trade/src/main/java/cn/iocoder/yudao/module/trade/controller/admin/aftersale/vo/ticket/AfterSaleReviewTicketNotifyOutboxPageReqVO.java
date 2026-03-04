package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 售后人工复核工单通知出站分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AfterSaleReviewTicketNotifyOutboxPageReqVO extends PageParam {

    @Schema(description = "工单 ID", example = "10001")
    private Long ticketId;

    @Schema(description = "通知状态（0 待发送 1 已发送 2 失败）", example = "0")
    private Integer status;

    @Schema(description = "通知类型", example = "SLA_WARN")
    private String notifyType;

    @Schema(description = "通知渠道", example = "IN_APP")
    private String channel;

    @Schema(description = "最近审计动作编码", example = "DISPATCH_FAILED")
    private String lastActionCode;

    @Schema(description = "最近审计业务号", example = "OUTBOX#1001@20260304153000")
    private String lastActionBizNo;

    @Schema(description = "最近审计动作时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] lastActionTime;
}
