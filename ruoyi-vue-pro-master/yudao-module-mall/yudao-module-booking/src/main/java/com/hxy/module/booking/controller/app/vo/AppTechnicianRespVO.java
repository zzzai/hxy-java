package com.hxy.module.booking.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户端 - 技师 Response VO")
@Data
public class AppTechnicianRespVO {

    @Schema(description = "技师编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "技师姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    @Schema(description = "头像", example = "https://xxx.com/avatar.jpg")
    private String avatar;

    @Schema(description = "简介", example = "资深按摩师")
    private String introduction;

    @Schema(description = "标签（逗号分隔）", example = "专业,耐心")
    private String tags;

    @Schema(description = "评分", example = "4.8")
    private BigDecimal rating;

    @Schema(description = "服务次数", example = "100")
    private Integer serviceCount;

}
