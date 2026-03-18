package com.hxy.module.booking.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 预约服务评价分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingReviewPageReqVO extends PageParam {

    @Schema(description = "评价ID", example = "1001")
    private Long id;

    @Schema(description = "预约订单ID", example = "2001")
    private Long bookingOrderId;

    @Schema(description = "门店ID", example = "3001")
    private Long storeId;

    @Schema(description = "技师ID", example = "4001")
    private Long technicianId;

    @Schema(description = "会员ID", example = "5001")
    private Long memberId;

    @Schema(description = "评价等级", example = "3")
    private Integer reviewLevel;

    @Schema(description = "风险等级", example = "2")
    private Integer riskLevel;

    @Schema(description = "跟进状态", example = "1")
    private Integer followStatus;

    @Schema(description = "只看店长待办", example = "true")
    private Boolean onlyManagerTodo;

    @Schema(description = "店长待办状态", example = "1")
    private Integer managerTodoStatus;

    @Schema(description = "店长待办 SLA 状态", example = "CLAIM_TIMEOUT")
    private String managerSlaStatus;

    @Schema(description = "是否已回复")
    private Boolean replyStatus;

    @Schema(description = "提交时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] submitTime;
}
