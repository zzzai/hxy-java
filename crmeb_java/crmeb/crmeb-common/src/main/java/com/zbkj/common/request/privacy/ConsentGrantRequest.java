package com.zbkj.common.request.privacy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * 用户授权请求
 */
@Data
@ApiModel(value = "ConsentGrantRequest对象", description = "用户授权请求")
public class ConsentGrantRequest {

    @ApiModelProperty(value = "授权场景编码", required = true, example = "HEALTH_BASIC")
    @NotBlank(message = "请填写授权场景")
    @Length(max = 64, message = "授权场景长度不能超过64")
    private String scenarioCode;

    @ApiModelProperty(value = "政策版本", required = true, example = "v1.0.0")
    @NotBlank(message = "请填写政策版本")
    @Length(max = 32, message = "政策版本长度不能超过32")
    private String policyVersion;

    @ApiModelProperty(value = "授权文案哈希", required = true)
    @NotBlank(message = "请填写授权文案哈希")
    @Length(max = 64, message = "授权文案哈希长度不能超过64")
    private String consentTextHash;

    @ApiModelProperty(value = "授权数据范围JSON")
    @Length(max = 4000, message = "授权数据范围过长")
    private String dataScopeJson;

    @ApiModelProperty(value = "用途编码列表JSON")
    @Length(max = 4000, message = "用途编码列表过长")
    private String purposeCodesJson;

    @ApiModelProperty(value = "过期时间（Unix秒）")
    private Integer expireAt;

    @ApiModelProperty(value = "来源渠道", example = "miniapp")
    @Length(max = 32, message = "来源渠道长度不能超过32")
    private String sourceChannel;

    @ApiModelProperty(value = "门店ID")
    private Integer storeId;
}

