package com.hxy.module.booking.controller.app.vo.feed;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "用户端 - 技师动态评论创建 Request VO")
@Data
public class AppTechnicianFeedCommentCreateReqVO {

    @Schema(description = "动态编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "动态编号不能为空")
    private Long postId;

    @Schema(description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "预约体验很好")
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 200, message = "评论内容不能超过 200 个字符")
    private String content;

    @Schema(description = "客户端幂等键", requiredMode = Schema.RequiredMode.REQUIRED, example = "comment-001")
    @NotBlank(message = "客户端幂等键不能为空")
    private String clientToken;
}
