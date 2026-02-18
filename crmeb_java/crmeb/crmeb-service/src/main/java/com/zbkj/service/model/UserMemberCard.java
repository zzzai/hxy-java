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
 * 用户会员卡表
 * 
 * @author CRMEB
 * @since 2026-02-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_user_member_card")
public class UserMemberCard implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 会员卡ID
     */
    private Integer cardId;

    /**
     * 卡号
     */
    private String cardNo;

    /**
     * 剩余次数
     */
    private Integer remainingTimes;

    /**
     * 剩余金额
     */
    private BigDecimal remainingAmount;

    /**
     * 生效时间
     */
    private Integer activeAt;

    /**
     * 过期时间
     */
    private Integer expireAt;

    /**
     * 状态：1=正常 2=已用完 3=已过期 4=已冻结
     */
    private Integer status;

    /**
     * 购买订单ID
     */
    private Long orderId;

    /**
     * 创建时间
     */
    private Integer createdAt;

    /**
     * 更新时间
     */
    private Integer updatedAt;
}


