package com.hxy.module.booking.controller.app.vo.feed;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "用户端 - 技师动态分页 Response VO")
@Data
@Accessors(chain = true)
public class AppTechnicianFeedPageRespVO {

    @Schema(description = "动态列表")
    private List<AppTechnicianFeedPostRespVO> list;

    @Schema(description = "是否还有更多", example = "false")
    private Boolean hasMore;

    @Schema(description = "下一页游标", example = "1000")
    private Long nextCursor;
}
