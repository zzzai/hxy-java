package com.zbkj.service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预约订单表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_booking_order")
public class BookingOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Integer uid;

    /**
     * 门店ID
     */
    private Integer storeId;

    /**
     * 技师ID
     */
    private Integer technicianId;

    /**
     * 排班ID
     */
    private Integer scheduleId;

    /**
     * 时间槽ID
     */
    private String slotId;

    /**
     * 服务SKU ID
     */
    private Integer serviceSkuId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 预约日期
     */
    private Date reserveDate;

    /**
     * 预约时间
     */
    private String reserveTime;

    /**
     * 服务时长（分钟）
     */
    private Integer serviceDuration;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

    /**
     * 实付价
     */
    private BigDecimal actualPrice;

    /**
     * 闲时优惠
     */
    private BigDecimal offpeakDiscount;

    /**
     * 支付方式：1=微信 2=支付宝 3=会员卡
     */
    private Integer paymentType;

    /**
     * 会员卡ID
     */
    private Long memberCardId;

    /**
     * 状态：1=待支付 2=已支付 3=已核销 4=已取消 5=已退款
     */
    private Integer status;

    /**
     * 核销码
     */
    private String checkInCode;

    /**
     * 核销时间
     */
    private Integer checkInTime;

    /**
     * 锁定过期时间
     */
    private Integer lockedExpire;

    /**
     * 创建时间
     */
    private Integer createdAt;

    /**
     * 更新时间
     */
    private Integer updatedAt;

    /**
     * 兼容历史代码字段
     */
    @TableField(exist = false)
    private Integer paidAt;

    /**
     * 兼容历史代码字段
     */
    @TableField(exist = false)
    private Integer completedAt;
}
