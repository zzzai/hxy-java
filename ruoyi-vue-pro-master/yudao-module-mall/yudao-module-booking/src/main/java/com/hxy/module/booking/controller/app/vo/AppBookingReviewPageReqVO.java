package com.hxy.module.booking.controller.app.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "用户端 - 预约服务评价分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppBookingReviewPageReqVO extends PageParam {

    @Schema(description = "评价等级过滤：1好评 2中评 3差评", example = "1")
    private Integer reviewLevel;
}
