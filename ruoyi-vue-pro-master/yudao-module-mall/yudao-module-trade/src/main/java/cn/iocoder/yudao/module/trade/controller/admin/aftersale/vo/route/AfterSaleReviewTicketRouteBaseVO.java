package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketRouteScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class AfterSaleReviewTicketRouteBaseVO {

    @Schema(description = "作用域：RULE/TYPE_SEVERITY/TYPE_DEFAULT/GLOBAL_DEFAULT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "作用域不能为空")
    @InEnum(value = AfterSaleReviewTicketRouteScopeEnum.class, message = "作用域必须是 {value}")
    private String scope;

    @Schema(description = "规则编码（RULE作用域必填）", example = "BLACKLIST_USER")
    @Size(max = 64, message = "规则编码长度不能超过 64")
    private String ruleCode;

    @Schema(description = "工单类型（TYPE_SEVERITY/TYPE_DEFAULT作用域必填）", example = "10")
    private Integer ticketType;

    @Schema(description = "严重级别（TYPE_SEVERITY作用域必填）", example = "P1")
    @Size(max = 16, message = "严重级别长度不能超过 16")
    private String severity;

    @Schema(description = "升级对象", requiredMode = Schema.RequiredMode.REQUIRED, example = "HQ_AFTER_SALE")
    @NotBlank(message = "升级对象不能为空")
    @Size(max = 64, message = "升级对象长度不能超过 64")
    private String escalateTo;

    @Schema(description = "SLA 分钟", requiredMode = Schema.RequiredMode.REQUIRED, example = "120")
    @NotNull(message = "SLA 分钟不能为空")
    @Min(value = 1, message = "SLA 分钟必须 >= 1")
    @Max(value = 10080, message = "SLA 分钟不能超过 10080")
    private Integer slaMinutes;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    @Schema(description = "排序（越小优先级越高）", example = "0")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "备注", example = "P0 风险单默认升级对象")
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

}
