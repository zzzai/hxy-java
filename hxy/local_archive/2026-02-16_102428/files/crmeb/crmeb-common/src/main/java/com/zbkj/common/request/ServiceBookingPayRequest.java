package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 服务预约支付请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "ServiceBookingPayRequest对象", description = "服务预约支付请求")
public class ServiceBookingPayRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "预约订单号", required = true)
    @NotBlank(message = "预约订单号不能为空")
    private String orderNo;

    @ApiModelProperty(value = "支付类型：weixin-微信支付", required = true)
    @NotBlank(message = "支付类型不能为空")
    private String payType;

    @ApiModelProperty(value = "支付渠道：weixinh5/public/routine", required = true)
    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;
}
