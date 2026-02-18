package com.zbkj.common.request.privacy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 数据访问工单审批请求
 */
@Data
@ApiModel(value = "DataAccessTicketApproveRequest对象", description = "数据访问工单审批请求")
public class DataAccessTicketApproveRequest {

    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "请选择工单")
    private Long ticketId;
}

