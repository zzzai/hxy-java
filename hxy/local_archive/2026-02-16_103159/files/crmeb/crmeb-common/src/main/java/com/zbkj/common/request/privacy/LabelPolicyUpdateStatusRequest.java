package com.zbkj.common.request.privacy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 标签策略状态更新请求
 */
@Data
@ApiModel(value = "LabelPolicyUpdateStatusRequest对象", description = "标签策略状态更新请求")
public class LabelPolicyUpdateStatusRequest {

    @ApiModelProperty(value = "策略ID", required = true)
    @NotNull(message = "请选择策略")
    private Long id;

    @ApiModelProperty(value = "启用状态：0=关闭 1=启用", required = true)
    @NotNull(message = "请填写启用状态")
    @Min(value = 0, message = "启用状态参数错误")
    @Max(value = 1, message = "启用状态参数错误")
    private Integer enabled;

    @ApiModelProperty(value = "备注")
    @Length(max = 255, message = "备注长度不能超过255")
    private String remarks;
}

