package com.hxy.module.booking.controller.app.vo.feed;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户端 - 技师动态点赞 Response VO")
@Data
@Accessors(chain = true)
public class AppTechnicianFeedLikeRespVO {

    @Schema(description = "动态编号", example = "1001")
    private Long postId;

    @Schema(description = "当前是否点赞", example = "true")
    private Boolean liked;

    @Schema(description = "最新点赞数", example = "4")
    private Integer likeCount;

    @Schema(description = "是否命中幂等", example = "false")
    private Boolean idempotentHit;
}
