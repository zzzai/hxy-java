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
 * 标签策略上线拦截规则
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_label_policy")
@ApiModel(value = "LabelPolicy对象", description = "标签策略上线拦截规则")
public class LabelPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "标签键")
    private String labelKey;

    @ApiModelProperty(value = "标签名称")
    private String labelName;

    @ApiModelProperty(value = "风险级别：1=G0 2=G1 3=G2")
    private Integer riskLevel;

    @ApiModelProperty(value = "是否启用")
    private Integer enabled;

    @ApiModelProperty(value = "是否需要人工复核")
    private Integer requireManualReview;

    @ApiModelProperty(value = "是否允许自动触达")
    private Integer allowAutoReach;

    @ApiModelProperty(value = "是否禁止自动决策")
    private Integer forbidAutoDecision;

    @ApiModelProperty(value = "用途白名单JSON")
    private String purposeWhitelistJson;

    @ApiModelProperty(value = "标签有效天数")
    private Integer expiryDays;

    @ApiModelProperty(value = "负责人")
    private String owner;

    @ApiModelProperty(value = "备注")
    private String remarks;

    @ApiModelProperty(value = "创建时间")
    private Integer createdAt;

    @ApiModelProperty(value = "更新时间")
    private Integer updatedAt;
}

