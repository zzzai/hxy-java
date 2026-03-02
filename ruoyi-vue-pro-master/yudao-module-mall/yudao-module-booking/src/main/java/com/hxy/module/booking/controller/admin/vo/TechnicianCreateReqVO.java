package com.hxy.module.booking.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 技师创建 Request VO")
@Data
public class TechnicianCreateReqVO {

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "门店编号不能为空")
    private Long storeId;

    @Schema(description = "关联用户编号", example = "100")
    private Long userId;

    @Schema(description = "技师姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "技师姓名不能为空")
    private String name;

    @Schema(description = "头像", example = "https://xxx.com/avatar.jpg")
    private String avatar;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "简介", example = "资深按摩师")
    private String introduction;

    @Schema(description = "标签（逗号分隔）", example = "专业,耐心")
    private String tags;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}
