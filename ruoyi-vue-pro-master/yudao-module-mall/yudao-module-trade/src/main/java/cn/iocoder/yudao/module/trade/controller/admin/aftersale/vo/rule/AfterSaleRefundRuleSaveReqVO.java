package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 管理后台 - 售后退款规则保存 Request VO
 */
@Schema(description = "管理后台 - 售后退款规则保存 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AfterSaleRefundRuleSaveReqVO extends AfterSaleRefundRuleBaseVO {
}
