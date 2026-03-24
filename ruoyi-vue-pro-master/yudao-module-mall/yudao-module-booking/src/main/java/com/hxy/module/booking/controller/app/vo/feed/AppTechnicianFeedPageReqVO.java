package com.hxy.module.booking.controller.app.vo.feed;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "用户端 - 技师动态分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppTechnicianFeedPageReqVO extends PageParam {

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "9")
    @NotNull(message = "门店编号不能为空")
    private Long storeId;

    @Schema(description = "技师编号", example = "88")
    private Long technicianId;

    @Schema(description = "最后一条动态编号，用于下拉续页", example = "1001")
    private Long lastId;
}
