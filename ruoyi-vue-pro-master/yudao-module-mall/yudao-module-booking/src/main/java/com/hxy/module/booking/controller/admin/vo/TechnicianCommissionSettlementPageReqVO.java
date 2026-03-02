package com.hxy.module.booking.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 技师佣金结算单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TechnicianCommissionSettlementPageReqVO extends PageParam {

    @Schema(description = "结算单号", example = "SET202603021200ABCD12")
    private String settlementNo;

    @Schema(description = "门店ID", example = "2001")
    private Long storeId;

    @Schema(description = "技师ID", example = "1001")
    private Long technicianId;

    @Schema(description = "状态（0草稿 10待审核 20已通过 30已驳回 40已作废 50已打款）", example = "10")
    private Integer status;

    @Schema(description = "审核人ID", example = "1")
    private Long reviewerId;

    @Schema(description = "是否已预警")
    private Boolean reviewWarned;

    @Schema(description = "是否已升级到P0")
    private Boolean reviewEscalated;

    @Schema(description = "审核 SLA 截止时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] reviewDeadlineTime;

    @Schema(description = "是否超时（仅待审核状态按截止时间判定）")
    private Boolean overdue;
}
