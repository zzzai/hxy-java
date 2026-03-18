package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 预约服务评价 Response VO")
@Data
public class BookingReviewRespVO {

    @Schema(description = "评价ID", example = "1")
    private Long id;

    @Schema(description = "预约订单ID", example = "2001")
    private Long bookingOrderId;

    @Schema(description = "服务履约单ID", example = "3001")
    private Long serviceOrderId;

    @Schema(description = "门店ID", example = "4001")
    private Long storeId;

    @Schema(description = "技师ID", example = "5001")
    private Long technicianId;

    @Schema(description = "会员ID", example = "6001")
    private Long memberId;

    @Schema(description = "服务商品SPU ID", example = "7001")
    private Long serviceSpuId;

    @Schema(description = "服务商品SKU ID", example = "8001")
    private Long serviceSkuId;

    @Schema(description = "总体评分", example = "2")
    private Integer overallScore;

    @Schema(description = "服务体验评分", example = "2")
    private Integer serviceScore;

    @Schema(description = "技师表现评分", example = "1")
    private Integer technicianScore;

    @Schema(description = "门店环境评分", example = "3")
    private Integer environmentScore;

    @Schema(description = "标签")
    private List<String> tags;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "评价图片")
    private List<String> picUrls;

    @Schema(description = "是否匿名")
    private Boolean anonymous;

    @Schema(description = "评价等级")
    private Integer reviewLevel;

    @Schema(description = "风险等级")
    private Integer riskLevel;

    @Schema(description = "展示状态")
    private Integer displayStatus;

    @Schema(description = "跟进状态")
    private Integer followStatus;

    @Schema(description = "是否已回复")
    private Boolean replyStatus;

    @Schema(description = "审核状态")
    private Integer auditStatus;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "服务完成时间")
    private LocalDateTime completedTime;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "差评触发类型")
    private String negativeTriggerType;

    @Schema(description = "店长联系人姓名")
    private String managerContactName;

    @Schema(description = "店长联系人手机号")
    private String managerContactMobile;

    @Schema(description = "店长待办状态")
    private Integer managerTodoStatus;

    @Schema(description = "店长待办认领截止时间")
    private LocalDateTime managerClaimDeadlineAt;

    @Schema(description = "店长待办首次处理截止时间")
    private LocalDateTime managerFirstActionDeadlineAt;

    @Schema(description = "店长待办闭环截止时间")
    private LocalDateTime managerCloseDeadlineAt;

    @Schema(description = "店长待办认领人")
    private Long managerClaimedByUserId;

    @Schema(description = "店长待办认领时间")
    private LocalDateTime managerClaimedAt;

    @Schema(description = "店长待办首次处理时间")
    private LocalDateTime managerFirstActionAt;

    @Schema(description = "店长待办闭环时间")
    private LocalDateTime managerClosedAt;

    @Schema(description = "店长待办最近处理备注")
    private String managerLatestActionRemark;

    @Schema(description = "店长待办最近处理人")
    private Long managerLatestActionByUserId;

    @Schema(description = "首次响应时间")
    private LocalDateTime firstResponseAt;

    @Schema(description = "跟进负责人")
    private Long followOwnerId;

    @Schema(description = "跟进结果")
    private String followResult;

    @Schema(description = "回复人")
    private Long replyUserId;

    @Schema(description = "回复内容")
    private String replyContent;

    @Schema(description = "回复时间")
    private LocalDateTime replyTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
