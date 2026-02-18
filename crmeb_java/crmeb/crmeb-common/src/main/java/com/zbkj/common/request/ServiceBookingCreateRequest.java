package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 服务预约下单请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "ServiceBookingCreateRequest对象", description = "服务预约下单请求")
public class ServiceBookingCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "排班ID", required = true)
    @NotNull(message = "排班ID不能为空")
    private Integer scheduleId;

    @ApiModelProperty(value = "时间槽ID", required = true)
    @NotBlank(message = "时间槽ID不能为空")
    private String slotId;

    @ApiModelProperty(value = "服务SKU ID", required = true)
    @NotNull(message = "服务SKU ID不能为空")
    private Integer serviceSkuId;

    @ApiModelProperty(value = "服务名称", required = true)
    @NotBlank(message = "服务名称不能为空")
    private String serviceName;

    @ApiModelProperty(value = "原价", required = true)
    @NotNull(message = "原价不能为空")
    @DecimalMin(value = "0.00", message = "原价不能小于0")
    private BigDecimal originalPrice;

    @ApiModelProperty(value = "实付价", required = true)
    @NotNull(message = "实付价不能为空")
    @DecimalMin(value = "0.00", message = "实付价不能小于0")
    private BigDecimal actualPrice;

    @ApiModelProperty(value = "支付方式：1=微信 2=支付宝 3=会员卡")
    private Integer paymentType;

    @ApiModelProperty(value = "会员卡ID（会员卡支付时必传）")
    private Long memberCardId;

    @ApiModelProperty(value = "幂等令牌（客户端生成）", required = true)
    @NotBlank(message = "幂等令牌不能为空")
    private String idempotentToken;
}
