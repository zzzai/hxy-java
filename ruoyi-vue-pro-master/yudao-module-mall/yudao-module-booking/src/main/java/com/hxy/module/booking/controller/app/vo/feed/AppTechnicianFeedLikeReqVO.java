package com.hxy.module.booking.controller.app.vo.feed;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "用户端 - 技师动态点赞 Request VO")
@Data
public class AppTechnicianFeedLikeReqVO {

    @Schema(description = "动态编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "动态编号不能为空")
    private Long postId;

    @Schema(description = "操作类型：1 点赞，0 取消点赞", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "操作类型不能为空")
    private Integer action;

    @Schema(description = "客户端幂等键", requiredMode = Schema.RequiredMode.REQUIRED, example = "like-001")
    @NotBlank(message = "客户端幂等键不能为空")
    private String clientToken;
}
