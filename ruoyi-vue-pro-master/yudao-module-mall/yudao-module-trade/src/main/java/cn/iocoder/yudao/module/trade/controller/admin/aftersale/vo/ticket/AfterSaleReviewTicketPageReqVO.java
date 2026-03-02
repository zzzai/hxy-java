package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketStatusEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 售后人工复核工单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AfterSaleReviewTicketPageReqVO extends PageParam {

    @Schema(description = "工单类型", example = "20")
    @InEnum(value = AfterSaleReviewTicketTypeEnum.class, message = "工单类型必须是 {value}")
    private Integer ticketType;

    @Schema(description = "售后单 ID", example = "1024")
    private Long afterSaleId;

    @Schema(description = "订单 ID", example = "2048")
    private Long orderId;

    @Schema(description = "用户 ID", example = "30001")
    private Long userId;

    @Schema(description = "规则编码", example = "AMOUNT_OVER_LIMIT")
    private String ruleCode;

    @Schema(description = "来源业务单号", example = "BK202603010001")
    private String sourceBizNo;

    @Schema(description = "严重级别", example = "P1")
    private String severity;

    @Schema(description = "升级对象", example = "HQ_AFTER_SALE")
    private String escalateTo;

    @Schema(description = "工单状态", example = "0")
    @InEnum(value = AfterSaleReviewTicketStatusEnum.class, message = "工单状态必须是 {value}")
    private Integer status;

    @Schema(description = "SLA 截止时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] slaDeadlineTime;

    @Schema(description = "收口时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] resolvedTime;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
