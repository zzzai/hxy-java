package com.hxy.module.booking.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 技师排班 DO（日维度）
 *
 * @author HXY
 */
@TableName("booking_technician_schedule")
@KeySequence("booking_technician_schedule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianScheduleDO extends BaseDO {

    /**
     * 排班编号
     */
    @TableId
    private Long id;

    /**
     * 技师编号
     */
    private Long technicianId;

    /**
     * 门店编号
     */
    private Long storeId;

    /**
     * 排班日期
     */
    private LocalDate scheduleDate;

    /**
     * 星期几：0=周日 1=周一...6=周六
     */
    private Integer weekDay;

    /**
     * 是否休息日：0=否 1=是
     */
    private Boolean isRestDay;

    /**
     * 上班时间
     */
    private LocalTime workStartTime;

    /**
     * 下班时间
     */
    private LocalTime workEndTime;

    /**
     * 预测预约率（智能推荐用）
     */
    private Integer predictedBookingRate;

    /**
     * 备注
     */
    private String remark;

}
