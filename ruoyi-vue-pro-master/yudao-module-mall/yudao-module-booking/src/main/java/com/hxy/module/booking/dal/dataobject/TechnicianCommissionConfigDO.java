package com.hxy.module.booking.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 技师佣金配置 DO（门店级）
 */
@TableName("technician_commission_config")
@KeySequence("technician_commission_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianCommissionConfigDO extends BaseDO {

    @TableId
    private Long id;

    /** 门店ID */
    private Long storeId;

    /** 佣金类型 */
    private Integer commissionType;

    /** 佣金比例 */
    private BigDecimal rate;

    /** 固定金额（分） */
    private Integer fixedAmount;

}
