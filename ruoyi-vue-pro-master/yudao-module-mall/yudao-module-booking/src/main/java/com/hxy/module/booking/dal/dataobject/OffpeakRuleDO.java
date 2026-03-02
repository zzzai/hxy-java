package com.hxy.module.booking.dal.dataobject;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalTime;

/**
 * 闲时规则 DO
 *
 * @author HXY
 */
@TableName("booking_offpeak_rule")
@KeySequence("booking_offpeak_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffpeakRuleDO extends BaseDO {

    /**
     * 规则编号
     */
    @TableId
    private Long id;

    /**
     * 门店编号
     */
    private Long storeId;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 适用星期（逗号分隔，如：1,2,3,4,5 表示周一到周五）
     */
    private String weekDays;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 折扣比例（如：80 表示8折）
     */
    private Integer discountRate;

    /**
     * 固定优惠价（分，优先于折扣比例）
     */
    private Integer fixedPrice;

    /**
     * 状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;

}
