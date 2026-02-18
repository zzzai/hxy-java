package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 服务预约核销请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "ServiceBookingVerifyRequest对象", description = "服务预约核销请求")
public class ServiceBookingVerifyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "预约订单号", required = true)
    @NotBlank(message = "预约订单号不能为空")
    private String orderNo;

    @ApiModelProperty(value = "核销码（可选，传入则会做一致性校验）")
    private String checkInCode;

    @ApiModelProperty(value = "核销门店ID（可选）")
    private Integer storeId;

    @ApiModelProperty(value = "核销技师ID（可选）")
    private Integer technicianId;

    @ApiModelProperty(value = "本次核销次数（次卡用，默认1）")
    private Integer usageTimes;

    @ApiModelProperty(value = "本次核销金额（储值卡用，默认订单实付金额）")
    private BigDecimal usageAmount;
}

