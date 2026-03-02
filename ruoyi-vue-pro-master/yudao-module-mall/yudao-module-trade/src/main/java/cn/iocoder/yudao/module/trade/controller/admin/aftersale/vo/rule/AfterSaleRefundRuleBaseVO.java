package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

/**
 * 退款规则 Base VO
 */
@Data
public class AfterSaleRefundRuleBaseVO {

    @Schema(description = "是否启用规则", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否启用规则不能为空")
    private Boolean enabled;

    @Schema(description = "自动退款金额上限（分）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5000")
    @NotNull(message = "自动退款金额上限不能为空")
    @PositiveOrZero(message = "自动退款金额上限不能为负数")
    private Integer autoRefundMaxPrice;

    @Schema(description = "用户当日售后申请次数阈值", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "用户当日售后申请次数阈值不能为空")
    @PositiveOrZero(message = "用户当日售后申请次数阈值不能为负数")
    private Integer userDailyApplyLimit;

    @Schema(description = "黑名单用户 ID 列表", example = "[10001, 10002]")
    private List<Long> blacklistUserIds;

    @Schema(description = "可疑订单关键字列表", example = "[\"TEST\", \"MOCK\"]")
    private List<String> suspiciousOrderKeywords;

    @Schema(description = "规则版本（可选，不填自动生成）", example = "v2")
    private String ruleVersion;

    @Schema(description = "备注", example = "HXY 风控规则")
    private String remark;

}
