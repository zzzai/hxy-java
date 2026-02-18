package com.zbkj.common.request.privacy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 撤回授权请求
 */
@Data
@ApiModel(value = "ConsentWithdrawRequest对象", description = "撤回授权请求")
public class ConsentWithdrawRequest {

    @ApiModelProperty(value = "授权ID", required = true)
    @NotNull(message = "请选择授权记录")
    private Long consentId;
}

