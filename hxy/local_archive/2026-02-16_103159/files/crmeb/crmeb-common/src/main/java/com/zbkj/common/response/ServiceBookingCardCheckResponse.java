package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 预约会员卡可用性检查响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "ServiceBookingCardCheckResponse对象", description = "预约会员卡可用性检查响应")
public class ServiceBookingCardCheckResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "预约订单号")
    private String orderNo;

    @ApiModelProperty(value = "会员卡ID")
    private Long memberCardId;

    @ApiModelProperty(value = "会员卡类型：1=次卡 2=储值卡 3=时长卡")
    private Integer cardType;

    @ApiModelProperty(value = "会员卡状态")
    private Integer cardStatus;

    @ApiModelProperty(value = "当前剩余值（次数或金额）")
    private BigDecimal remainingValue;

    @ApiModelProperty(value = "本次所需值（次数或金额）")
    private BigDecimal requiredValue;

    @ApiModelProperty(value = "是否可用")
    private Boolean available;

    @ApiModelProperty(value = "不可用原因码")
    private String reasonCode;

    @ApiModelProperty(value = "原因描述")
    private String reasonMessage;

    @ApiModelProperty(value = "过期时间（时间戳）")
    private Integer expireTime;
}

