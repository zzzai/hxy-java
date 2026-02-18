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
 * 数据访问工单
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_data_access_ticket")
@ApiModel(value = "DataAccessTicket对象", description = "数据访问工单")
public class DataAccessTicket implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "工单号")
    private String ticketNo;

    @ApiModelProperty(value = "租户ID")
    private Integer tenantId;

    @ApiModelProperty(value = "被访问用户ID")
    private Integer userId;

    @ApiModelProperty(value = "申请人ID")
    private Integer applicantId;

    @ApiModelProperty(value = "申请人角色：1=技师 2=店长 3=客服 4=管理员")
    private Integer applicantRole;

    @ApiModelProperty(value = "数据级别：1=L1 2=L2 3=L3 4=L4")
    private Integer dataLevel;

    @ApiModelProperty(value = "访问字段列表JSON")
    private String dataFieldsJson;

    @ApiModelProperty(value = "使用目的编码")
    private String purposeCode;

    @ApiModelProperty(value = "访问理由")
    private String reason;

    @ApiModelProperty(value = "是否需要审批")
    private Integer approvalRequired;

    @ApiModelProperty(value = "状态：0=SUBMITTED 1=AUTO_PASS 2=APPROVED 3=REJECTED 4=EXECUTED 5=CLOSED")
    private Integer status;

    @ApiModelProperty(value = "审批人ID")
    private Integer approverId;

    @ApiModelProperty(value = "审批时间")
    private Integer approvedAt;

    @ApiModelProperty(value = "驳回时间")
    private Integer rejectedAt;

    @ApiModelProperty(value = "驳回原因")
    private String rejectReason;

    @ApiModelProperty(value = "工单过期时间")
    private Integer expireAt;

    @ApiModelProperty(value = "链路追踪ID")
    private String traceId;

    @ApiModelProperty(value = "创建时间")
    private Integer createdAt;

    @ApiModelProperty(value = "更新时间")
    private Integer updatedAt;
}

