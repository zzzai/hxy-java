package com.zbkj.common.model.booking;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预约持久化对象
 * 
 * 对应数据库表：eb_booking
 * 
 * @author 荷小悦架构师
 * @date 2026-02-12
 */
@Data
@TableName("eb_booking")
public class BookingPO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    /**
     * 预约编号
     */
    private String bookingNo;
    
    /**
     * 用户ID
     */
    private Integer userId;
    
    /**
     * 门店ID
     */
    private Integer storeId;
    
    /**
     * 服务ID
     */
    private Integer serviceId;
    
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * 技师ID
     */
    private Integer technicianId;
    
    /**
     * 技师姓名
     */
    private String technicianName;
    
    /**
     * 技师技能等级
     */
    private Integer technicianSkillLevel;
    
    /**
     * 预约时间
     */
    private Date bookingTime;
    
    /**
     * 服务时长（分钟）
     */
    private Integer durationMinutes;
    
    /**
     * 时间槽ID
     */
    private Integer timeSlotId;
    
    /**
     * 状态 0-待确认 1-已确认 2-服务中 3-已完成 4-已取消 5-超时取消
     */
    private Integer status;
    
    /**
     * 原价
     */
    private BigDecimal originalPrice;
    
    /**
     * 实付金额
     */
    private BigDecimal payAmount;
    
    /**
     * 关联订单ID
     */
    private Integer orderId;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 支付时间
     */
    private Date payTime;
    
    /**
     * 服务开始时间
     */
    private Date serviceStartTime;
    
    /**
     * 服务完成时间
     */
    private Date serviceEndTime;
    
    /**
     * 取消时间
     */
    private Date cancelTime;
    
    /**
     * 取消原因
     */
    private String cancelReason;
    
    /**
     * 更新时间
     */
    private Date updateTime;
}


