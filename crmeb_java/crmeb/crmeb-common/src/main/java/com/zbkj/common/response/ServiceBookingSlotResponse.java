package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 服务预约时间槽响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "ServiceBookingSlotResponse对象", description = "服务预约时间槽响应")
public class ServiceBookingSlotResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "排班ID")
    private Integer scheduleId;

    @ApiModelProperty(value = "时间槽ID")
    private String slotId;

    @ApiModelProperty(value = "开始时间")
    private String startTime;

    @ApiModelProperty(value = "结束时间")
    private String endTime;

    @ApiModelProperty(value = "状态：available/locked/booked")
    private String status;

    @ApiModelProperty(value = "价格")
    private BigDecimal price;

    @ApiModelProperty(value = "是否闲时")
    private Boolean offpeak;
}
