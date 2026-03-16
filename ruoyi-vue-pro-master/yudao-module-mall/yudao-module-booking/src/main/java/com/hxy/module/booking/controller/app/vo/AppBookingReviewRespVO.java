package com.hxy.module.booking.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "用户端 - 预约服务评价 Response VO")
@Data
public class AppBookingReviewRespVO {

    @Schema(description = "评价ID", example = "9001")
    private Long id;

    @Schema(description = "预约订单ID", example = "1001")
    private Long bookingOrderId;

    @Schema(description = "服务履约单ID", example = "2001")
    private Long serviceOrderId;

    @Schema(description = "门店ID", example = "3001")
    private Long storeId;

    @Schema(description = "技师ID", example = "4001")
    private Long technicianId;

    @Schema(description = "服务商品SPU ID", example = "5001")
    private Long serviceSpuId;

    @Schema(description = "服务商品SKU ID", example = "6001")
    private Long serviceSkuId;

    @Schema(description = "总体评分", example = "5")
    private Integer overallScore;

    @Schema(description = "服务体验评分", example = "5")
    private Integer serviceScore;

    @Schema(description = "技师表现评分", example = "5")
    private Integer technicianScore;

    @Schema(description = "门店环境评分", example = "5")
    private Integer environmentScore;

    @Schema(description = "标签", example = "[\"服务专业\",\"环境整洁\"]")
    private List<String> tags;

    @Schema(description = "评价内容", example = "整体体验不错")
    private String content;

    @Schema(description = "评价图片", example = "[\"https://example.com/review-1.png\"]")
    private List<String> picUrls;

    @Schema(description = "是否匿名", example = "false")
    private Boolean anonymous;

    @Schema(description = "评价等级", example = "1")
    private Integer reviewLevel;

    @Schema(description = "风险等级", example = "0")
    private Integer riskLevel;

    @Schema(description = "展示状态", example = "0")
    private Integer displayStatus;

    @Schema(description = "跟进状态", example = "0")
    private Integer followStatus;

    @Schema(description = "是否已回复", example = "false")
    private Boolean replyStatus;

    @Schema(description = "审核状态", example = "0")
    private Integer auditStatus;

    @Schema(description = "来源", example = "order_detail")
    private String source;

    @Schema(description = "服务完成时间")
    private LocalDateTime completedTime;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "回复人ID", example = "1")
    private Long replyUserId;

    @Schema(description = "回复内容", example = "感谢反馈，我们已安排回访")
    private String replyContent;

    @Schema(description = "回复时间")
    private LocalDateTime replyTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
