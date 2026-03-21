package com.hxy.module.booking.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 预约服务评价通知出站分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingReviewNotifyOutboxPageReqVO extends PageParam {

    @Schema(description = "评价ID", example = "1001")
    private Long reviewId;

    @Schema(description = "门店ID", example = "2001")
    private Long storeId;

    @Schema(description = "接收角色", example = "STORE_MANAGER")
    private String receiverRole;

    @Schema(description = "接收账号ID", example = "3001")
    private Long receiverUserId;

    @Schema(description = "通知状态", example = "PENDING")
    private String status;

    @Schema(description = "通知渠道", example = "IN_APP")
    private String channel;

    @Schema(description = "通知类型", example = "NEGATIVE_REVIEW_CREATED")
    private String notifyType;

    @Schema(description = "最近动作编码", example = "MANUAL_RETRY")
    private String lastActionCode;
}
