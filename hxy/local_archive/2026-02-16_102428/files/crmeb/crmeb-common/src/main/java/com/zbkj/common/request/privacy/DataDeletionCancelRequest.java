package com.zbkj.common.request.privacy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 数据删除撤销请求
 */
@Data
@ApiModel(value = "DataDeletionCancelRequest对象", description = "数据删除撤销请求")
public class DataDeletionCancelRequest {

    @ApiModelProperty(value = "删除工单ID", required = true)
    @NotNull(message = "请选择删除工单")
    private Long ticketId;
}

