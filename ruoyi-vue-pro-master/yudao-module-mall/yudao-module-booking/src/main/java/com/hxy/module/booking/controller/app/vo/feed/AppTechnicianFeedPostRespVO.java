package com.hxy.module.booking.controller.app.vo.feed;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "用户端 - 技师动态条目 Response VO")
@Data
@Accessors(chain = true)
public class AppTechnicianFeedPostRespVO {

    @Schema(description = "动态编号", example = "1001")
    private Long postId;

    @Schema(description = "门店编号", example = "9")
    private Long storeId;

    @Schema(description = "技师编号", example = "88")
    private Long technicianId;

    @Schema(description = "动态标题", example = "本周技师手法精选")
    private String title;

    @Schema(description = "动态内容", example = "今天状态不错")
    private String content;

    @Schema(description = "封面图", example = "https://example.com/feed-cover.png")
    private String coverUrl;

    @Schema(description = "点赞数", example = "4")
    private Integer likeCount;

    @Schema(description = "评论数", example = "2")
    private Integer commentCount;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;
}
