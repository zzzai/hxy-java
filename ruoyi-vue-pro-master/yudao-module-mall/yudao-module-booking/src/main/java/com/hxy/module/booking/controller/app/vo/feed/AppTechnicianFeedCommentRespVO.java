package com.hxy.module.booking.controller.app.vo.feed;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户端 - 技师动态评论 Response VO")
@Data
@Accessors(chain = true)
public class AppTechnicianFeedCommentRespVO {

    @Schema(description = "评论编号", example = "7001")
    private Long commentId;

    @Schema(description = "动态编号", example = "1001")
    private Long postId;

    @Schema(description = "审核状态", example = "REVIEWING")
    private String status;

    @Schema(description = "是否降级", example = "false")
    private Boolean degraded;
}
