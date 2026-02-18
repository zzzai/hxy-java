package com.zbkj.common.request.privacy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 数据访问工单驳回请求
 */
@Data
@ApiModel(value = "DataAccessTicketRejectRequest对象", description = "数据访问工单驳回请求")
public class DataAccessTicketRejectRequest {

    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "请选择工单")
    private Long ticketId;

    @ApiModelProperty(value = "驳回原因", required = true)
    @NotBlank(message = "请填写驳回原因")
    @Length(max = 255, message = "驳回原因长度不能超过255")
    private String rejectReason;
}

