package com.hxy.module.booking.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.hxy.module.booking.enums.TimeSlotStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 时间槽 DO（小时维度）
 *
 * @author HXY
 */
@TableName("booking_time_slot")
@KeySequence("booking_time_slot_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDO extends BaseDO {

    /**
     * 时间槽编号
     */
    @TableId
    private Long id;

    /**
     * 排班编号
     */
    private Long scheduleId;

    /**
     * 技师编号
     */
    private Long technicianId;

    /**
     * 门店编号
     */
    private Long storeId;

    /**
     * 日期
     */
    private LocalDate slotDate;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 时长（分钟）
     */
    private Integer duration;

    /**
     * 是否闲时：0=否 1=是
     */
    private Boolean isOffpeak;

    /**
     * 闲时价格（分）
     */
    private Integer offpeakPrice;

    /**
     * 状态
     *
     * 枚举 {@link TimeSlotStatusEnum}
     */
    private Integer status;

    /**
     * 锁定过期时间（用于并发控制）
     */
    private LocalDateTime lockExpireTime;

    /**
     * 锁定用户编号
     */
    private Long lockUserId;

    /**
     * 预约订单编号
     */
    private Long bookingOrderId;

}
