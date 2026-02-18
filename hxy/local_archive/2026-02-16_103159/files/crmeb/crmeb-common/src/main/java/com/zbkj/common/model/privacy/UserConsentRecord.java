package com.zbkj.common.model.privacy;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 用户授权记录
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_user_consent_record")
@ApiModel(value = "UserConsentRecord对象", description = "用户授权记录")
public class UserConsentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "租户ID")
    private Integer tenantId;

    @ApiModelProperty(value = "用户ID")
    private Integer userId;

    @ApiModelProperty(value = "门店ID")
    private Integer storeId;

    @ApiModelProperty(value = "场景编码")
    private String scenarioCode;

    @ApiModelProperty(value = "授权数据范围JSON")
    private String dataScopeJson;

    @ApiModelProperty(value = "用途编码列表JSON")
    private String purposeCodesJson;

    @ApiModelProperty(value = "政策版本")
    private String policyVersion;

    @ApiModelProperty(value = "授权文案哈希")
    private String consentTextHash;

    @ApiModelProperty(value = "状态：0=PENDING 1=GRANTED 2=DENIED 3=WITHDRAWN 4=EXPIRED")
    private Integer consentStatus;

    @ApiModelProperty(value = "授权时间")
    private Integer grantedAt;

    @ApiModelProperty(value = "撤回时间")
    private Integer withdrawnAt;

    @ApiModelProperty(value = "过期时间")
    private Integer expireAt;

    @ApiModelProperty(value = "来源渠道")
    private String sourceChannel;

    @ApiModelProperty(value = "操作人类型：1=用户 2=员工 3=系统")
    private Integer operatorType;

    @ApiModelProperty(value = "操作人ID")
    private Integer operatorId;

    @ApiModelProperty(value = "链路追踪ID")
    private String traceId;

    @ApiModelProperty(value = "创建时间")
    private Integer createdAt;

    @ApiModelProperty(value = "更新时间")
    private Integer updatedAt;
}

