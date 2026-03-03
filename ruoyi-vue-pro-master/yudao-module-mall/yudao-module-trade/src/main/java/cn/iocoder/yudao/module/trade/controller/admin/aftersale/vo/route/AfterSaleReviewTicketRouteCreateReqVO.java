package cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 售后复核工单路由规则创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AfterSaleReviewTicketRouteCreateReqVO extends AfterSaleReviewTicketRouteBaseVO {
}
