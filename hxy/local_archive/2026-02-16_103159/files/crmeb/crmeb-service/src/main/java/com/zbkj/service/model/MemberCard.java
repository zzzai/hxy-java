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
 * 会员卡表
 * 
 * @author CRMEB
 * @since 2026-02-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_member_card")
public class MemberCard implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Integer uid;

    /**
     * 会员卡名称
     */
    private String cardName;

    /**
     * 卡类型：1=次卡 2=储值卡 3=时长卡
     */
    private Integer cardType;

    /**
     * 总价值（金额/次数/时长）
     */
    private BigDecimal totalValue;

    /**
     * 剩余价值
     */
    private BigDecimal remainingValue;

    /**
     * 状态：1=正常 2=冻结 3=已用完 4=已过期
     */
    private Integer status;

    /**
     * 过期时间
     */
    private Integer expireTime;

    /**
     * 创建时间
     */
    private Integer createdAt;

    /**
     * 更新时间
     */
    private Integer updatedAt;
}

