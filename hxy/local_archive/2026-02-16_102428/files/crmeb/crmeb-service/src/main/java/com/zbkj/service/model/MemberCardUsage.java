package com.zbkj.service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 会员卡使用记录表
 * 
 * @author CRMEB
 * @since 2026-02-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_member_card_usage")
public class MemberCardUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户会员卡ID
     */
    private Long userCardId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 使用类型：1=全额核销 2=部分核销
     */
    private Integer usageType;

    /**
     * 使用次数
     */
    private Integer usedTimes;

    /**
     * 使用金额
     */
    private BigDecimal usedAmount;

    /**
     * 使用前剩余次数
     */
    private Integer beforeTimes;

    /**
     * 使用后剩余次数
     */
    private Integer afterTimes;

    /**
     * 使用前剩余金额
     */
    private BigDecimal beforeAmount;

    /**
     * 使用后剩余金额
     */
    private BigDecimal afterAmount;

    /**
     * 核销门店ID
     */
    private Integer storeId;

    /**
     * 核销技师ID
     */
    private Integer technicianId;

    /**
     * 创建时间
     */
    private Integer createdAt;
}


