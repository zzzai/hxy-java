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
 * 数据删除工单
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_data_deletion_ticket")
@ApiModel(value = "DataDeletionTicket对象", description = "数据删除工单")
public class DataDeletionTicket implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "删除工单号")
    private String ticketNo;

    @ApiModelProperty(value = "租户ID")
    private Integer tenantId;

    @ApiModelProperty(value = "用户ID")
    private Integer userId;

    @ApiModelProperty(value = "删除范围")
    private String scopeCode;

    @ApiModelProperty(value = "删除范围JSON")
    private String scopeJson;

    @ApiModelProperty(value = "状态：0=SUBMITTED 1=COOLING 2=EXECUTING 3=COMPLETED 4=REJECTED 5=FAILED 6=CANCELED")
    private Integer status;

    @ApiModelProperty(value = "冷静期结束时间")
    private Integer coolingUntil;

    @ApiModelProperty(value = "申请时间")
    private Integer requestedAt;

    @ApiModelProperty(value = "执行完成时间")
    private Integer executedAt;

    @ApiModelProperty(value = "法定保留：0=否 1=是")
    private Integer legalHold;

    @ApiModelProperty(value = "法定保留原因")
    private String holdReason;

    @ApiModelProperty(value = "执行结果摘要")
    private String resultSummary;

    @ApiModelProperty(value = "执行人ID")
    private Integer operatorId;

    @ApiModelProperty(value = "创建时间")
    private Integer createdAt;

    @ApiModelProperty(value = "更新时间")
    private Integer updatedAt;
}

