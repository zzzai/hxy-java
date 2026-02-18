package com.zbkj.common.request.privacy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * 数据删除申请请求
 */
@Data
@ApiModel(value = "DataDeletionRequest对象", description = "数据删除申请请求")
public class DataDeletionRequest {

    @ApiModelProperty(value = "删除范围编码", required = true, example = "HEALTH_DATA")
    @NotBlank(message = "请填写删除范围")
    @Length(max = 64, message = "删除范围长度不能超过64")
    private String scopeCode;

    @ApiModelProperty(value = "删除范围JSON")
    @Length(max = 4000, message = "删除范围配置过长")
    private String scopeJson;
}

