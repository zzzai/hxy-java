package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 服务预约订单响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "ServiceBookingOrderResponse对象", description = "服务预约订单响应")
public class ServiceBookingOrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "预约订单号")
    private String orderNo;

    @ApiModelProperty(value = "订单状态：1=待支付 2=已支付 3=已核销 4=已取消 5=已退款")
    private Integer status;

    @ApiModelProperty(value = "排班ID")
    private Integer scheduleId;

    @ApiModelProperty(value = "时间槽ID")
    private String slotId;

    @ApiModelProperty(value = "预约日期（yyyy-MM-dd）")
    private String reserveDate;

    @ApiModelProperty(value = "预约时间段（HH:mm-HH:mm）")
    private String reserveTime;

    @ApiModelProperty(value = "实付金额")
    private BigDecimal actualPrice;

    @ApiModelProperty(value = "核销码")
    private String checkInCode;
}
