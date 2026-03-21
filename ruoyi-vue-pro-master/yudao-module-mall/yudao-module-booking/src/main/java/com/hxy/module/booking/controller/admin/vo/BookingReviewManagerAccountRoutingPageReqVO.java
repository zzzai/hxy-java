package com.hxy.module.booking.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 预约评价店长账号路由分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingReviewManagerAccountRoutingPageReqVO extends PageParam {

    @Schema(description = "门店ID", example = "3001")
    private Long storeId;

    @Schema(description = "门店名称", example = "朝阳门店")
    private String storeName;

    @Schema(description = "联系人手机号", example = "13900000000")
    private String contactMobile;
}
