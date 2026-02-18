package com.zbkj.common.request.privacy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 数据访问工单创建请求
 */
@Data
@ApiModel(value = "DataAccessTicketCreateRequest对象", description = "数据访问工单创建请求")
public class DataAccessTicketCreateRequest {

    @ApiModelProperty(value = "用户ID", required = true)
    @NotNull(message = "请选择用户")
    private Integer userId;

    @ApiModelProperty(value = "申请人角色：1=技师 2=店长 3=客服 4=管理员", required = true)
    @NotNull(message = "请填写申请人角色")
    @Min(value = 1, message = "角色参数错误")
    @Max(value = 4, message = "角色参数错误")
    private Integer applicantRole;

    @ApiModelProperty(value = "数据级别：1=L1 2=L2 3=L3 4=L4", required = true)
    @NotNull(message = "请填写数据级别")
    @Min(value = 1, message = "数据级别参数错误")
    @Max(value = 4, message = "数据级别参数错误")
    private Integer dataLevel;

    @ApiModelProperty(value = "访问字段JSON")
    @Length(max = 4000, message = "访问字段配置过长")
    private String dataFieldsJson;

    @ApiModelProperty(value = "使用目的编码", required = true)
    @NotBlank(message = "请填写使用目的")
    @Length(max = 64, message = "使用目的长度不能超过64")
    private String purposeCode;

    @ApiModelProperty(value = "访问理由")
    @Length(max = 255, message = "访问理由长度不能超过255")
    private String reason;
}

