package com.hxy.module.booking.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 技师佣金记录 DO
 */
@TableName("technician_commission")
@KeySequence("technician_commission_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianCommissionDO extends BaseDO {

    @TableId
    private Long id;

    /** 技师ID */
    private Long technicianId;

    /** 预约订单ID */
    private Long orderId;

    /** 用户ID */
    private Long userId;

    /** 门店ID */
    private Long storeId;

    /** 佣金类型 1=基础 2=点钟 3=加钟 4=卡项销售 5=商品 6=好评 */
    private Integer commissionType;

    /** 订单金额（分） */
    private Integer baseAmount;

    /** 佣金比例 */
    private BigDecimal commissionRate;

    /** 佣金金额（分） */
    private Integer commissionAmount;

    /** 业务类型（冲正幂等键） */
    private String bizType;

    /** 业务单号（冲正幂等键） */
    private String bizNo;

    /** 归属员工ID（冲正幂等键） */
    private Long staffId;

    /** 原佣金ID（冲正幂等键） */
    private Long originCommissionId;

    /** 状态 0=待结算 1=已结算 2=已取消 */
    private Integer status;

    /** 结算单ID */
    private Long settlementId;

    /** 结算时间 */
    private LocalDateTime settlementTime;

}
