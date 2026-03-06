package com.hxy.module.booking.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约订单 DO
 *
 * @author HXY
 */
@TableName("booking_order")
@KeySequence("booking_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingOrderDO extends BaseDO {

    /**
     * 预约订单编号
     */
    @TableId
    private Long id;

    /**
     * 预约订单号
     */
    private String orderNo;

    /**
     * 用户编号
     */
    private Long userId;

    /**
     * 门店编号
     */
    private Long storeId;

    /**
     * 技师编号
     */
    private Long technicianId;

    /**
     * 时间槽编号
     */
    private Long timeSlotId;

    /**
     * 服务商品SPU编号
     */
    private Long spuId;

    /**
     * 服务商品SKU编号
     */
    private Long skuId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务图片
     */
    private String servicePic;

    /**
     * 预约日期
     */
    private LocalDate bookingDate;

    /**
     * 预约开始时间
     */
    private LocalTime bookingStartTime;

    /**
     * 预约结束时间
     */
    private LocalTime bookingEndTime;

    /**
     * 服务时长（分钟）
     */
    private Integer duration;

    /**
     * 原价（分）
     */
    private Integer originalPrice;

    /**
     * 优惠金额（分）
     */
    private Integer discountPrice;

    /**
     * 实付金额（分）
     */
    private Integer payPrice;

    /**
     * 是否闲时优惠
     */
    private Boolean isOffpeak;

    /**
     * 订单状态
     *
     * 枚举 {@link BookingOrderStatusEnum}
     */
    private Integer status;

    /**
     * 支付订单编号
     */
    private Long payOrderId;

    /**
     * 退款单编号
     */
    private Long payRefundId;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 退款时间
     */
    private LocalDateTime refundTime;

    /**
     * 服务开始时间
     */
    private LocalDateTime serviceStartTime;

    /**
     * 服务结束时间
     */
    private LocalDateTime serviceEndTime;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 用户备注
     */
    private String userRemark;

    /**
     * 商家备注
     */
    private String merchantRemark;

    /**
     * 派单模式：1=点钟（用户指定技师），2=排钟（系统分配）
     */
    private Integer dispatchMode;

    /**
     * 父订单ID（加钟/升级时关联原订单）
     */
    private Long parentOrderId;

    /**
     * 是否加钟子订单：0=正常订单，1=加钟子订单
     */
    private Integer isAddon;

    /**
     * 加钟类型：1=加钟，2=升级，3=加项目
     */
    private Integer addonType;

}
