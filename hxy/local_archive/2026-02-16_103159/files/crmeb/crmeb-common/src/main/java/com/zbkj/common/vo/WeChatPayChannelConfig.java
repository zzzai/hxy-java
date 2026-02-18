package com.zbkj.common.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 微信支付渠道配置（含服务商模式）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "WeChatPayChannelConfig对象", description = "微信支付渠道配置")
public class WeChatPayChannelConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "支付渠道编码:0公众号,1小程序,2H5,4/5 App")
    private Integer channel;

    @ApiModelProperty(value = "门店ID")
    private Integer storeId;

    @ApiModelProperty(value = "渠道原始应用ID(历史配置)")
    private String baseAppId;

    @ApiModelProperty(value = "微信请求使用的appId")
    private String appId;

    @ApiModelProperty(value = "前端拉起支付使用的appId")
    private String clientAppId;

    @ApiModelProperty(value = "微信请求使用的商户号")
    private String mchId;

    @ApiModelProperty(value = "微信签名Key")
    private String signKey;

    @ApiModelProperty(value = "退款证书路径")
    private String certificatePath;

    @ApiModelProperty(value = "是否服务商模式")
    private Boolean serviceProviderMode;

    @ApiModelProperty(value = "子商户号")
    private String subMchId;

    @ApiModelProperty(value = "子商户应用ID")
    private String subAppId;
}

