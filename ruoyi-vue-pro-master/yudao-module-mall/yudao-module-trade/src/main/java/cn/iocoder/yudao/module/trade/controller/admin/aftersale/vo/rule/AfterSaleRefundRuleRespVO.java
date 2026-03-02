package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 管理后台 - 售后退款规则 Response VO
 */
@Schema(description = "管理后台 - 售后退款规则 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AfterSaleRefundRuleRespVO extends AfterSaleRefundRuleBaseVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "规则来源（DB / YAML）", example = "DB")
    private String source;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
