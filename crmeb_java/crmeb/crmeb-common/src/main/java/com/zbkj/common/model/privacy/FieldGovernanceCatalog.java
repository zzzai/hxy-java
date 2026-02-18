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
 * 字段治理目录
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_field_governance_catalog")
@ApiModel(value = "FieldGovernanceCatalog对象", description = "字段治理目录")
public class FieldGovernanceCatalog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "字段编码")
    private String fieldCode;

    @ApiModelProperty(value = "字段名称")
    private String fieldName;

    @ApiModelProperty(value = "数据域")
    private String domain;

    @ApiModelProperty(value = "敏感级别：1=L1 2=L2 3=L3 4=L4")
    private Integer sensitivityLevel;

    @ApiModelProperty(value = "必要性：1=必需 2=增强 3=冻结")
    private Integer necessityLevel;

    @ApiModelProperty(value = "用途编码")
    private String purposeCode;

    @ApiModelProperty(value = "法律基础")
    private String legalBasis;

    @ApiModelProperty(value = "是否需要单独同意")
    private Integer consentRequired;

    @ApiModelProperty(value = "保留天数")
    private Integer retentionDays;

    @ApiModelProperty(value = "默认启用：0=否 1=是")
    private Integer defaultEnabled;

    @ApiModelProperty(value = "备注")
    private String remarks;

    @ApiModelProperty(value = "创建时间")
    private Integer createdAt;

    @ApiModelProperty(value = "更新时间")
    private Integer updatedAt;
}

