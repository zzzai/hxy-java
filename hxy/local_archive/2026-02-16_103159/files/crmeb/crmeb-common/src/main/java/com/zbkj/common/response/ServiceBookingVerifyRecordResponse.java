package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 预约核销记录响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "ServiceBookingVerifyRecordResponse对象", description = "预约核销记录响应")
public class ServiceBookingVerifyRecordResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "核销流水ID")
    private Long usageId;

    @ApiModelProperty(value = "预约订单号")
    private String orderNo;

    @ApiModelProperty(value = "会员卡ID")
    private Long memberCardId;

    @ApiModelProperty(value = "核销类型：1=次数 2=金额")
    private Integer usageType;

    @ApiModelProperty(value = "本次使用次数")
    private Integer usedTimes;

    @ApiModelProperty(value = "本次使用金额")
    private BigDecimal usedAmount;

    @ApiModelProperty(value = "核销前余额")
    private BigDecimal beforeAmount;

    @ApiModelProperty(value = "核销后余额")
    private BigDecimal afterAmount;

    @ApiModelProperty(value = "核销门店ID")
    private Integer storeId;

    @ApiModelProperty(value = "核销技师ID")
    private Integer technicianId;

    @ApiModelProperty(value = "创建时间（时间戳）")
    private Integer createdAt;
}

