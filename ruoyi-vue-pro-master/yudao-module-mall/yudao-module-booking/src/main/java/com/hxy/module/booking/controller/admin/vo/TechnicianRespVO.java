package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 技师 Response VO")
@Data
public class TechnicianRespVO {

    @Schema(description = "技师编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long storeId;

    @Schema(description = "关联用户编号", example = "100")
    private Long userId;

    @Schema(description = "技师姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    @Schema(description = "头像", example = "https://xxx.com/avatar.jpg")
    private String avatar;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "简介", example = "资深按摩师")
    private String introduction;

    @Schema(description = "标签（逗号分隔）", example = "专业,耐心")
    private String tags;

    @Schema(description = "评分", example = "4.8")
    private BigDecimal rating;

    @Schema(description = "服务次数", example = "100")
    private Integer serviceCount;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
